package com.kiteclass.core.module.course.validator;

import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Validator for course prerequisite relationships.
 *
 * <p>Detects circular dependencies using Depth-First Search (DFS) algorithm.
 *
 * <p>Circular dependency examples:
 * <ul>
 *   <li>Self-prerequisite: Course A → Course A</li>
 *   <li>Direct cycle: Course A → Course B → Course A</li>
 *   <li>Transitive cycle: Course A → Course B → Course C → Course A</li>
 * </ul>
 *
 * <p>Algorithm Complexity: O(V + E) where V = number of courses, E = number of prerequisite relationships
 *
 * @author KiteClass Team
 * @since 2.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrerequisiteValidator {

    private final CourseRepository courseRepository;

    /**
     * Checks if adding a prerequisite would create a circular dependency.
     *
     * <p>Uses DFS to traverse the prerequisite graph starting from the prerequisite course.
     * If traversal reaches the target course, a cycle is detected.
     *
     * <p>Example:
     * <pre>
     * Existing: A → B → C
     * Adding: C → A
     * Result: Cycle detected (A → B → C → A)
     * </pre>
     *
     * @param courseId ID of course to add prerequisite to
     * @param prerequisiteId ID of prerequisite to add
     * @return true if circular dependency detected, false otherwise
     */
    public boolean wouldCreateCycle(Long courseId, Long prerequisiteId) {
        if (courseId.equals(prerequisiteId)) {
            log.warn("Course {} cannot be its own prerequisite (self-cycle)", courseId);
            return true; // Self-prerequisite
        }

        Set<Long> visited = new HashSet<>();
        boolean hasCycle = dfs(prerequisiteId, courseId, visited);

        if (hasCycle) {
            log.warn("Adding prerequisite {} to course {} would create circular dependency",
                prerequisiteId, courseId);
        }

        return hasCycle;
    }

    /**
     * Depth-First Search (DFS) traversal to detect cycles in prerequisite graph.
     *
     * <p>Traverses from current node towards target node.
     * If target found, cycle exists. If already visited, no cycle in this path.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>If current equals target: cycle found (return true)</li>
     *   <li>If current already visited: no cycle in this path (return false)</li>
     *   <li>Mark current as visited</li>
     *   <li>Recursively check all prerequisites of current</li>
     * </ol>
     *
     * @param current Current node (course ID) being visited
     * @param target Target node (course ID) to search for
     * @param visited Set of visited nodes to prevent infinite loops
     * @return true if target found (cycle detected), false otherwise
     */
    private boolean dfs(Long current, Long target, Set<Long> visited) {
        // Base case 1: Cycle found
        if (current.equals(target)) {
            return true;
        }

        // Base case 2: Already visited this path, no cycle
        if (visited.contains(current)) {
            return false;
        }

        // Mark as visited
        visited.add(current);

        // Fetch current course (deleted courses are filtered by repository)
        Course currentCourse = courseRepository.findByIdAndDeletedFalse(current).orElse(null);
        if (currentCourse == null) {
            return false; // Course not found or deleted, no cycle
        }

        // Recursively check all prerequisites of current course
        for (Course prereq : currentCourse.getPrerequisiteCourses()) {
            if (dfs(prereq.getId(), target, visited)) {
                return true; // Cycle found in this prerequisite path
            }
        }

        return false; // No cycle found
    }
}
