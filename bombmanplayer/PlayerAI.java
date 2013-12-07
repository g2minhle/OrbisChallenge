import static com.orbischallenge.bombman.api.game.MapItems.*;

import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.jboss.netty.util.EstimatableObjectWrapper;

/**
 * 
 * @author c.sham
 */
public class PlayerAI implements Player {

	List<Point> allBlocks;
	Random rnd = new Random();
	int dy[] = { -1, 0, 1, 0 };
	int dx[] = { 0, 1, 0, -1 };
	int row, column;
	PlayerAction movePlayerAction[] = { Move.up.action, Move.right.action,
			Move.down.action, Move.left.action };

	/**
	 * Gets called every time a new game starts.
	 * 
	 * @param map
	 *            The map.
	 * @param blocks
	 *            All the blocks on the map.
	 * @param players
	 *            Current position, bomb range, and bomb count for both Bombers.
	 * @param playerIndex
	 *            Your player index.
	 */
	@Override
	public void newGame(MapItems[][] map, List<Point> blocks, Bomber[] players,
			int playerIndex) {
		column = map.length;
		row = map[0].length;
		allBlocks = blocks;
	}

	private boolean isSafe(Point currentPosition,
			HashMap<Point, Bomb> bombLocations, MapItems[][] map) {
		System.out.println("Checking if in danger");
		Set<Point> locations = bombLocations.keySet();
		System.out.println("Boom location size " + locations.size());
		for (Point bombLocation : locations) {
			System.out.println("Boom location " + bombLocation.x + "."
					+ bombLocation.y);
			int x, y;
			Bomb bomb = bombLocations.get(bombLocation);
			int bRange = bomb.getRange();
			System.out.println("Boom range " + bRange);
			for (int i = 0; i < 4; i++) {
				for (int d = 0; d <= bRange; d++) {
					x = bombLocation.x + d * dx[i];
					y = bombLocation.y + d * dy[i];
					if (validAddress(x, y)) {
						if (map[x][y].isWalkable() || d == 0) {
							if (x == currentPosition.x
									&& y == currentPosition.y) {
								return false;
							}
						} else {
							break;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Gets called every time a move is requested from the game server.
	 * 
	 * Provided is a very random and not smart AI which random moves without
	 * checking for explosions, and places bombs whenever bombs can be used to
	 * destroy blocks.
	 * 
	 * @param map
	 *            The current map
	 * @param bombLocations
	 *            Bombs currently on the map and it's range, owner and time left
	 *            Exploding bombs are excluded.
	 * @param powerUpLocations
	 *            Power-ups current on the map and it's type
	 * @param players
	 *            Current position, bomb range, and bomb count for both Bombers
	 * @param explosionLocations
	 *            Explosions currently on the map.
	 * @param playerIndex
	 *            Your player index.
	 * @param moveNumber
	 *            The current move number.
	 * @return the PlayerAction you want your Bomber to perform.
	 */
	@Override
	public PlayerAction getMove(MapItems[][] map,
			HashMap<Point, Bomb> bombLocations,
			HashMap<Point, PowerUps> powerUpLocations, Bomber[] players,
			List<Point> explosionLocations, int playerIndex, int moveNumber) {
		/**
		 * Keep track of which blocks are destroyed
		 */
		for (Point explosions : explosionLocations) {
			if (allBlocks.contains(explosions)) {
				allBlocks.remove(explosions);
			}
		}
		Bomber curPlayer = players[playerIndex];
		Point curPosition = curPlayer.position;
		boolean GTFO = false;
		PlayerAction action;

		if (isSafe(curPosition, bombLocations, map)) {
			System.out.println("No danger");
			// check if we near a block
			boolean nearBlock = false;
			for (int i = 0; i < 4; i++) {
				int x = curPosition.x + dx[i];
				int y = curPosition.y + dy[i];
				Point next = new Point(x, y);
				if (validAddress(x, y)) {
					if (allBlocks.contains(next)) {
						nearBlock = true;
						break;
					}
				}
			}
			if (!nearBlock) {
				LinkedList<Point> pathToNearestBlocks = findNearestBlock(	
						curPosition, map, bombLocations, powerUpLocations,explosionLocations);
				if (pathToNearestBlocks != null) {
					System.out.println("There is a valid path");
					LinkedList<PlayerAction> path = getPathFromPoints(pathToNearestBlocks);
					if (path.size() > 0) {
						action = path.get(0);
					} else {
						action = Move.still.action;
					}
				} else {
					System.out.println("No path found");
					action = Move.still.action;
				}
			} else {
				action = Move.still.bombaction.PLACEBOMB;
			}
		} else {
			System.out.println("In danger");
			GTFO = true;
			LinkedList<Point> pathToNearestBlocks = findSafestBlock(
					curPosition, map, bombLocations);
			if (pathToNearestBlocks != null) {
				System.out.println("There is a valid path");
				LinkedList<PlayerAction> path = getPathFromPointsSafe(pathToNearestBlocks);
				if (path.size() > 0) {
					action = path.get(0);
				} else {
					action = Move.still.action;
				}
			} else {
				System.out.println("No path found");
				action = Move.still.action;
			}
		}
		if (action == Move.still.bombaction.PLACEBOMB) {
			Bomb newBomb = new Bomb(0, curPlayer.bombRange, 0);
			bombLocations.put(curPosition, newBomb);
			LinkedList<Point> pathToNearestBlocks = findSafestBlock(
					curPosition, map, bombLocations);
			if (pathToNearestBlocks == null) {
				action = Move.still.action;
			}
		} else if (action != Move.still.action) {
			Point nextPoint = getNextPoint(action, curPosition);
			if (!isSafe(nextPoint, bombLocations, map) && !GTFO) {
				action = Move.still.action;
			}
			if (explosionLocations.contains(nextPoint)) {
				action = Move.still.action;
			}
		}
		System.out.println("Do action " + action);
		return action;
	}

	private Point getNextPoint(PlayerAction action, Point curPosition) {
		for (int i = 0; i < 4; i++) {
			if (action == movePlayerAction[i]) {
				int x = curPosition.x + dx[i];
				int y = curPosition.y + dy[i];
				Point result = new Point(x, y);
				return result;
			}
		}
		return null;
	}

	private boolean validAddress(int x, int y) {
		if (x < 0)
			return false;
		if (y < 0)
			return false;
		if (x >= column)
			return false;
		if (y >= row)
			return false;
		return true;
	}

	/**
	 * Uses Breadth First Search to find if a walkable path from point A to
	 * point B exists.
	 * 
	 * This method does not consider the if tiles are dangerous or not. As long
	 * as all the tiles in are walkable.
	 * 
	 * @param start
	 *            The starting point
	 * @param end
	 *            The end point
	 * @param map
	 *            The map use to check if a path exists between point A and
	 *            point B
	 * @return True if there is a walkable path between point A and point B,
	 *         False otherwise.
	 */
	public LinkedList<Point> findNearestBlock(Point start, MapItems[][] map,
			HashMap<Point, Bomb> bombLocations,HashMap<Point, PowerUps> powerUpLocations, List<Point> explosionLocations) {
		// Keeps track of points we have to check
		int currentHead = 0;
		LinkedList<Point> queue = new LinkedList<Point>();
		LinkedList<Integer> trace = new LinkedList<Integer>();
		// Keeps track of points we have already visited
		LinkedList<Point> visited = new LinkedList<Point>();
		queue.add(start);
		trace.add(-1);
		while (currentHead < queue.size()) {
			Point curPoint = queue.get(currentHead);
			System.out.println("Dequeue: " + curPoint.x + "." + curPoint.y);
			currentHead++;
			// Check all the neighbours of the current point in question
			for (int i = 0; i < 4; i++) {
				int x = curPoint.x + dx[i];
				int y = curPoint.y + dy[i];
				System.out.println("Checking: " + x + "." + y);
				Point next = new Point(x, y);
				if (validAddress(x, y)) {
					if (!visited.contains(next)) {
						if (isSafe(next, bombLocations, map) && !explosionLocations.contains(next)) {
							if (allBlocks.contains(next) || (powerUpLocations.containsKey(next) && map[x][y].isWalkable()) ) {
								// Found the thing
								System.out.println("Good spot: " + x + "." + y);
								int father = currentHead - 1;
								LinkedList<Point> result = new LinkedList<Point>();
								result.add(next);
								System.out.println("Add: " + next.x + "."
										+ next.y);
								Point currentPoint;
								while (father != -1) {
									System.out.println("Father : " + father);
									currentPoint = queue.get(father);
									System.out.println("Add: " + currentPoint.x
											+ "." + currentPoint.y);
									result.addFirst(currentPoint);
									father = trace.get(father);
								}
								// return the path
								return result;
							} else if (map[x][y].isWalkable()) {
								System.out.println("Enqueue: " + x + "." + y);
								queue.addLast(next);
								visited.addLast(next);
								trace.add(currentHead - 1);
							}
						}
					}
				}
			}
		}

		return null;
	}

	public LinkedList<Point> findSafestBlock(Point start, MapItems[][] map,
			HashMap<Point, Bomb> bombLocations) {
		// Keeps track of points we have to check
		int currentHead = 0;
		LinkedList<Point> queue = new LinkedList<Point>();
		LinkedList<Integer> trace = new LinkedList<Integer>();
		// Keeps track of points we have already visited
		LinkedList<Point> visited = new LinkedList<Point>();
		queue.add(start);
		trace.add(-1);
		while (currentHead < queue.size()) {
			Point curPoint = queue.get(currentHead);
			System.out.println("Dequeue: " + curPoint.x + "." + curPoint.y);
			currentHead++;
			// Check all the neighbours of the current point in question
			for (int i = 0; i < 4; i++) {
				int x = curPoint.x + dx[i];
				int y = curPoint.y + dy[i];
				System.out.println("Checking: " + x + "." + y);
				Point next = new Point(x, y);
				if (validAddress(x, y)) {
					if (!visited.contains(next)) {
						if (map[x][y].isWalkable()
								&& isSafe(next, bombLocations, map)) {
							// Found the thing
							System.out.println("Good spot: " + x + "." + y);
							int father = currentHead - 1;
							LinkedList<Point> result = new LinkedList<Point>();
							result.add(next);
							System.out.println("Add: " + next.x + "." + next.y);
							Point currentPoint;
							while (father != -1) {
								System.out.println("Father : " + father);
								currentPoint = queue.get(father);
								System.out.println("Add: " + currentPoint.x
										+ "." + currentPoint.y);
								result.addFirst(currentPoint);
								father = trace.get(father);
							}
							// return the path
							return result;
						} else if (map[x][y].isWalkable()) {
							System.out.println("Enqueue: " + x + "." + y);
							queue.addLast(next);
							visited.addLast(next);
							trace.add(currentHead - 1);
						}
					}
				}
			}
		}

		return null;
	}

	private PlayerAction connectNeighbourPoints(Point A, Point B) {
		if (A.x + 1 == B.x) {
			return Move.right.action;
		} else if (A.y + 1 == B.y) {
			return Move.down.action;
		} else if (A.y - 1 == B.y) {
			return Move.up.action;
		} else {
			return Move.left.action;
		}
	}

	private LinkedList<PlayerAction> getPathFromPointsSafe(
			LinkedList<Point> points) {
		int numPoints = points.size();
		LinkedList<PlayerAction> Path = new LinkedList<PlayerAction>();
		for (int i = 0; i < numPoints - 1; i++) {
			System.out.println("Points: " + points.get(i).x + "."
					+ points.get(i).y);
			Point p = points.get(i);
			Point neighbour = points.get(i + 1);
			Path.addLast(connectNeighbourPoints(p, neighbour));
		}
		return Path;
	}

	private LinkedList<PlayerAction> getPathFromPoints(LinkedList<Point> points) {
		int n = points.size() - 1;
		LinkedList<PlayerAction> Path = new LinkedList<PlayerAction>();
		for (int i = 0; i < n; i++) {
			Point p = points.get(i);
			Point neighbour = points.get(i + 1);
			System.out.println("Points " + i + ":" + p.x + "." + p.y);
			System.out.println("Points next to " + i + ":" + neighbour.x + "."
					+ neighbour.y);
			Path.addLast(connectNeighbourPoints(p, neighbour));
		}
		return Path;
	}
}
