import static com.orbischallenge.bombman.api.game.MapItems.*;

import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;

import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.logging.Log;

/**
 * 
 * @author c.sham
 */
public class PlayerAI implements Player {

	List<Point> allBlocks;
	Random rnd = new Random();
	int dx[] = { -1, 0, 1, 0 };
	int dy[] = { 0, 1, 0, -1 };
	int row;
	int column;
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
		allBlocks = blocks;
		row = map.length;
		column = map[0].length;
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
		LinkedList<PlayerAction> validMoves = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			int x = curPosition.x + dx[i];
			int y = curPosition.y + dy[i];
			if (validAddress(x, y)) {
				if (map[x][y].isWalkable()) {
					validMoves.add(movePlayerAction[i]);
					System.out.print("--------------------" + validMoves.size()
							+ "----------------------\n");
					System.out.print("--------------------" + i
							+ "----------------------\n");
				}
			}
		}
		if (validMoves.isEmpty()) {
			return Move.still.action;
		}
		int moveRndPiacked =  rnd.nextInt();
		moveRndPiacked = Math.abs(moveRndPiacked) % validMoves.size();
		System.out.print("+++++++++++++++++++++" + moveRndPiacked+ "+++++++++++++++++++++++\n");
		System.out.print("+++++++++++++++++++++" + validMoves.get(moveRndPiacked)+ "+++++++++++++++++++++++\n");
		return validMoves.get(moveRndPiacked);

	}

	private boolean validAddress(int x, int y) {
		if (x < 0)
			return false;
		if (y < 0)
			return false;
		if (x >= row)
			return false;
		if (y >= column)
			return false;
		return true;
	}
}
