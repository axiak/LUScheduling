package org.learningu.scheduling.flags;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public final class Flags {
  private Flags() {}

  public static void addFlagBinding(
      Binder binder,
      final Flag flagAnnotation,
      final TypeLiteral<?> parameterType) {
    MapBinder.newMapBinder(binder, Flag.class, TypeLiteral.class)
        .addBinding(flagAnnotation)
        .toInstance(parameterType);
  }

  public static void addFlagBindings(Binder binder, Class<?>... classes) {
    for (Class<?> clazz : classes) {
      addFlagBindings(binder, clazz);
    }
  }

  public static void addFlagBindings(Binder binder, TypeLiteral<?> literal) {
    for (Field field : literal.getRawType().getFields()) {
      if (field.isAnnotationPresent(Flag.class)) {
        Flag annot = field.getAnnotation(Flag.class);
        addFlagBinding(binder, annot, literal.getFieldType(field));
      }
    }
    for (Constructor<?> constructor : literal.getRawType().getConstructors()) {
      List<TypeLiteral<?>> parameterTypes = literal.getParameterTypes(constructor);
      Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
      for (int i = 0; i < parameterTypes.size(); i++) {
        Annotation[] annotations = parameterAnnotations[i];
        TypeLiteral<?> typ = parameterTypes.get(i);
        for (Annotation annot : annotations) {
          if (annot instanceof Flag) {
            addFlagBinding(binder, (Flag) annot, typ);
          }
        }
      }
    }
  }

  public static Injector bootstrapFlagInjector(final String[] args, Module... baseModules) {
    Injector baseInjector = Guice.createInjector(baseModules);
    return baseInjector.createChildInjector(
        baseInjector.getInstance(FlagBootstrapModule.class),
        new AbstractModule() {

          @Override
          protected void configure() {}

          @SuppressWarnings("unused")
          @Provides
          @RuntimeArguments
          String[] commandLineArguments() {
            return args;
          }

          @SuppressWarnings({ "unused", "rawtypes" })
          @Provides
          Options options(Map<Flag, TypeLiteral> flagsMap) {
            Options options = new Options();
            for (Flag flag : flagsMap.keySet()) {
              OptionBuilder.hasArgs();
              OptionBuilder.withArgName(flag.name());
              OptionBuilder.withLongOpt(flag.name());
              options.addOption(OptionBuilder.create());
            }
            return options;
          }

          @SuppressWarnings("unused")
          @Provides
          @Singleton
          CommandLine commandLine(Options options, String[] args) {
            try {
              return new PosixParser().parse(options, args);
            } catch (ParseException e) {
              throw Throwables.propagate(e);
            }
          }
        });
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static final class FlagBootstrapModule extends AbstractModule {
    private final CommandLine commandLine;
    private final Map<Flag, TypeLiteral> flagsMap;

    @SuppressWarnings("unused")
    // injected
    @Inject
    FlagBootstrapModule(CommandLine commandLine, Map<Flag, TypeLiteral> flagsMap) {
      this.commandLine = commandLine;
      this.flagsMap = flagsMap;
    }

    @Override
    protected void configure() {
      for (Map.Entry<Flag, TypeLiteral> entry : flagsMap.entrySet()) {
        Flag flagAnnotation = entry.getKey();
        TypeLiteral literal = entry.getValue();

        @Nullable
        String value = commandLine.getOptionValue(flagAnnotation.name());

        bind(literal).annotatedWith(flagAnnotation).toInstance(
            Converters.converterFor(literal).parse(value));
      }
    }
  }
}
