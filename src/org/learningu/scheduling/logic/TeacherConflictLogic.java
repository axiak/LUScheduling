package org.learningu.scheduling.logic;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Map.Entry;
import java.util.Set;

import org.learningu.scheduling.graph.Program;
import org.learningu.scheduling.graph.Room;
import org.learningu.scheduling.graph.Section;
import org.learningu.scheduling.graph.Teacher;
import org.learningu.scheduling.schedule.PresentAssignment;
import org.learningu.scheduling.schedule.Schedule;

/**
 * Logic for verifying that an assignment would not require that teachers teach two classes at
 * once.
 * 
 * @author lowasser
 * 
 */
public final class TeacherConflictLogic extends ScheduleLogic {

  @Override
  public
      void
      validate(ScheduleValidator validator, Schedule schedule, PresentAssignment assignment) {
    super.validate(validator, schedule, assignment);
    Program program = schedule.getProgram();
    /*
     * Collecting the set of all courses taught by the same teachers is more efficient than going
     * through every teacher who is teaching a class this period.
     */
    Set<Teacher> teachers = program.teachersFor(assignment.getSection());
    Set<Section> coursesTaughtBySame = coursesTaughtByTeachers(program, teachers);
    Set<PresentAssignment> conflicts = Sets.newLinkedHashSet();
    for (Entry<Room, PresentAssignment> entry : schedule
        .occurringAt(assignment.getPeriod())
        .entrySet()) {
      PresentAssignment assign = entry.getValue();
      Section course = assign.getSection();
      if (coursesTaughtBySame.contains(course)) {
        conflicts.add(assign);
      }
    }

    validator.validateGlobal(
        assignment,
        conflicts,
        "Teachers must not be assigned to teach more than one class at a time");
  }

  static Set<Section> coursesTaughtByTeachers(Program program, Iterable<Teacher> teachers) {
    ImmutableSet.Builder<Section> builder = ImmutableSet.builder();
    for (Teacher t : teachers) {
      builder.addAll(program.getSectionsForTeacher(t));
    }
    return builder.build();
  }
}
