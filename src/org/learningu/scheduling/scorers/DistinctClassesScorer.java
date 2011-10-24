package org.learningu.scheduling.scorers;

import java.util.Set;

import org.learningu.scheduling.Schedule;
import org.learningu.scheduling.StartAssignment;
import org.learningu.scheduling.graph.Course;
import org.learningu.scheduling.optimization.Scorer;

import com.google.common.collect.Sets;

/**
 * Scorer that scores schedules based on how many distinct classes were scheduled.
 * 
 * @author lowasser
 */
public final class DistinctClassesScorer implements Scorer<Schedule> {

  @Override
  public double score(Schedule input) {
    Set<Course> scheduled = Sets
        .newHashSetWithExpectedSize(input.getProgram().getCourses().size());
    for (StartAssignment assign : input.startAssignments()) {
      scheduled.add(assign.getCourse());
    }
    return scheduled.size();
  }

}
