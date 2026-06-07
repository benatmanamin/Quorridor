package modele;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/*
 * Cherche s'il existe un chemin entre un joueur et sa ligne d'arrivée.
 * On utilise BFS pour trouver le plus court chemin.
 */
public class PathFinder implements Serializable {
    private static final long serialVersionUID = 1L;

    public boolean hasPathToGoal(GameState state, Player player) {
        return shortestPathLength(state, player) != -1;
    }

    public int shortestPathLength(GameState state, Player player) {
        List<Position> path = shortestPath(state, player);
        return path.isEmpty() ? -1 : path.size() - 1;
    }

    public List<Position> shortestPath(GameState state, Player player) {
        Board board = state.getBoard();
        Queue<Position> queue = new LinkedList<>();
        Map<Position, Integer> distance = new HashMap<>();
        Map<Position, Position> previous = new HashMap<>();

        Position start = player.getPosition();
        queue.add(start);
        distance.put(start, 0);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            int currentDistance = distance.get(current);

            if (current.getRow() == player.getGoalRow()) {
                return buildPath(previous, current);
            }

            for (Direction direction : Direction.values()) {
                Position next = current.move(direction);

                if (!board.isInside(next)) continue;
                if (board.isBlocked(current, next)) continue;
                if (distance.containsKey(next)) continue;

                distance.put(next, currentDistance + 1);
                previous.put(next, current);
                queue.add(next);
            }
        }

        return Collections.emptyList();
    }

    private List<Position> buildPath(Map<Position, Position> previous, Position goal) {
        List<Position> path = new ArrayList<>();
        Position current = goal;

        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }

        Collections.reverse(path);
        return path;
    }
}
