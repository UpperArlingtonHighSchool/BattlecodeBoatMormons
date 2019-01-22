package bc19;

import java.util.ArrayList;
import java.util.LinkedList;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	private boolean hRefl; // true means vertical axis of symm, false is opposite
	private int castleIndex;
	private int top;
	private int left;
	private int numUnits = 0;
	private int numMines = 0;
	private int numCastles = 0;
	private ArrayList<int[]> karbosInUse = new ArrayList<>();
	private ArrayList<int[]> fuelsInUse = new ArrayList<>();
	private ArrayList<int[]> currentPath = new ArrayList<>();
	private int[][] fullMap;
	private int[][] robotMap;
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private int[] HOME;
	private int[] castleIDs = new int[3];
	// Matrix of adjacent spaces, relative to the Robot
	private final int[][] adjacentSpaces = new int[][] { new int[] { 0, 1 }, new int[] { -1, 1 }, new int[] { -1, 0 },
			new int[] { -1, -1 }, new int[] { 0, -1 }, new int[] { 1, -1 }, new int[] { 1, 0 }, new int[] { 1, 1 } };

	@Override
	public Action turn() {
		if (me.turn == 1) {
			getFullMap();
		}
		robotMap = getVisibleRobotMap();
		switch (me.unit) {
		case 0: // castle
			return castleAction();
		case 1: // church
			break;
		case 2: // pilgrim
			return pilgrimAction();
		case 3: // crusader
			break;
		case 4: // prophet
			break;
		case 5: // preacher
			break;
		}
		return null;
	}

	private Action castleAction() {
		if (me.turn == 1) {
			numCastles = 1;
			castleIDs[0] = me.id;

			for (Robot rob : getVisibleRobots()) {
				if (rob.team == me.team && rob.id != me.id) {
					castleIDs[numCastles] = rob.id;
					numCastles += 1;
				}
			}
		}
		for (int i = 0; i < numCastles; i++) {
			int castleID = castleIDs[i];
			if (castleID == -1) {
				continue;
			}
			Robot castle = getRobot(castleID);
			if (castle == null) {
				castleIDs[i] = -1;
				continue;
			}
			numUnits += castle.castle_talk;
		}
		log("global population: " + numUnits);
		boolean haveNeighbors = false;
		for (int[] move : adjacentSpaces) {
			int tryX = me.x + move[0];
			int tryY = me.y + move[1];
			if (tryX <= -1 || tryX >= fullMap[0].length || tryY <= -1 || tryY >= fullMap.length) {
				continue;
			}
			if (robotMap[tryY][tryX] > 0) {
				haveNeighbors = true;
				break;
			}
		}
		if (haveNeighbors) {
			signal(numUnits, 2);
		}
		if (numUnits >= numMines || fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}
		for (int[] move : adjacentSpaces) {
			int buildX = me.x + move[0];
			int buildY = me.y + move[1];
			if (buildX <= -1 || buildX >= fullMap[0].length || buildY <= -1 || buildY >= fullMap.length
					|| fullMap[buildY][buildX] == IMPASSABLE || robotMap[buildY][buildX] > 0) {
				continue;
			}
			numUnits++;
			// signal(numUnits, 2);
			castleTalk(1);
			return buildUnit(SPECS.PILGRIM, move[0], move[1]);
		}
		return null;
	}

	private Action pilgrimAction() {
		if (me.turn == 1) {
			HOME = new int[] { me.x, me.y };
		}
		Robot castle = null;
		for (int[] move : adjacentSpaces) {
			int testX = me.x + move[0];
			int testY = me.y + move[1];
			if (testX <= -1 || testX >= fullMap[0].length || testY <= -1 || testY >= fullMap.length) {
				continue;
			}
			Robot maybe = getRobot(robotMap[testY][testX]);
			if (robotMap[testY][testX] > 0 && maybe.unit == SPECS.CASTLE && maybe.team == me.team) {
				castle = maybe;
				karbosInUse.clear();
				fuelsInUse.clear();
				if (isRadioing(castle)) {
					numUnits = castle.signal;
				}
			}
		}

		if (currentPath != null && currentPath.size() > 0) {
			int[] nextMove = currentPath.get(0);
			int dx = nextMove[0] - me.x;
			int dy = nextMove[1] - me.y;
			if (robotMap[nextMove[1]][nextMove[0]] <= 0) {
				if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE) {
					currentPath.remove(0);
					return move(dx, dy);
				}
			}
		}

		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY
				|| me.fuel == SPECS.UNITS[SPECS.PILGRIM].FUEL_CAPACITY) {
			if (castle != null) {
				return give(castle.x - me.x, castle.y - me.y, me.karbonite, me.fuel);
			}

			currentPath = bfsCooties(HOME[0], HOME[1]);
			if (currentPath == null) {
				return null;
			}
			int[] nextMove = currentPath.get(0);
			int dx = nextMove[0] - me.x;
			int dy = nextMove[1] - me.y;
			if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE) {
				currentPath.remove(0);
				return move(dx, dy);
			}
			return null;
		}
		if (fullMap[me.y][me.x] == KARBONITE || fullMap[me.y][me.x] == FUEL) {
			if (fuel == 0) {
				return null;
			}
			return mine();
		}
		int[] location;
		if (20 * numUnits > fuel) {
			location = findClosestFuel();
		} else {
			location = findClosestKarbo();
		}

		if (location == null) {
			location = HOME;
		}

		currentPath = bfsCooties(location[0], location[1]);
		if (currentPath == null) {
			return null;
		}
		int[] nextMove = currentPath.get(0);
		int dx = nextMove[0] - me.x;
		int dy = nextMove[1] - me.y;
		if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE) {
			currentPath.remove(0);
			return move(dx, dy);
		}
		return null;
	}

	public void getFullMap() {
		boolean[][] p = getPassableMap();
		boolean[][] k = getKarboniteMap();
		boolean[][] f = getFuelMap();

		fullMap = new int[p.length][p[0].length];

		int h = p.length;
		int w = p[0].length;

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				if (!p[i][j]) {
					fullMap[i][j] = IMPASSABLE;
				} else if (k[i][j]) {
					fullMap[i][j] = KARBONITE;
					numMines++;
				} else if (f[i][j]) {
					fullMap[i][j] = FUEL;
					numMines++;
				} else {
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	private boolean getReflectionDir() {
		int top = (fullMap.length + 1) / 2;
		int left = (fullMap[0].length + 1) / 2;

		for (int i = 0; i < top; i++) {
			for (int j = 0; j < left; j++) {
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		for (int i = fullMap.length; i > top; i--) {
			for (int j = fullMap[0].length; j > left; j--) {
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		return true;
	}

	private int[] findClosestKarbo() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = null;
		for (int x = 0; x < fullMap[0].length; x++) {
			looping: for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE) {
					int[] temp = new int[] { x, y };
					for (int[] out : karbosInUse) {
						if (out[0] == temp[0] && out[1] == temp[1]) {
							if (robotMap[y][x] == 0) {
								karbosInUse.remove(out);
							} else {
								continue looping;
							}
						}
					}
					if (robotMap[y][x] > 0) {
						karbosInUse.add(temp);
						continue looping;
					}
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans = temp;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}

	private int[] findClosestFuel() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = null;
		for (int x = 0; x < fullMap[0].length; x++) {
			looping: for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE) {
					int[] temp = new int[] { x, y };
					for (int[] out : fuelsInUse) {
						if (out[0] == temp[0] && out[1] == temp[1]) {
							if (robotMap[y][x] == 0) {
								fuelsInUse.remove(out);
							} else {
								continue looping;
							}
						}
					}
					if (robotMap[y][x] > 0) {
						fuelsInUse.add(temp);
						continue looping;
					}
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans = temp;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}

	// bfs is reaaaally fast now
	private ArrayList<int[]> bfs(int goalX, int goalY) {
		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap[0].length];
		for (int i = 0; i < from.length; i++) {
			from[i] = -1;
		}

		// these two are only used if occupied == true
		int[] closestSpot = null;
		int closestDistance = (goalX - me.x) * (goalX - me.x) + (goalY - me.y) * (goalY - me.y);

		while (!(spot[0] == goalX && spot[1] == goalY)) {
			int left = Math.max(0, spot[0] - maxRadius);
			int top = Math.max(0, spot[1] - maxRadius);
			int right = Math.min(fullMap[0].length - 1, spot[0] + maxRadius);
			int bottom = Math.min(fullMap.length - 1, spot[1] + maxRadius);

			for (int x = left; x <= right; x++) {
				int dx = x - spot[0];
				for (int y = top; y <= bottom; y++) {
					int dy = y - spot[1];
					if (dx * dx + dy * dy <= maxRadius * maxRadius && fullMap[y][x] > IMPASSABLE
							&& robotMap[y][x] <= 0) {
						if (from[y * fullMap.length + x] != -1) {
							continue;
						}
						int[] newSpot = new int[] { x, y };
						from[y * fullMap.length + x] = spot[1] * fullMap.length + spot[0];

						if (occupied) {
							if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
								closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
								closestSpot = newSpot;
								continue;
							}
						}
						spots.add(newSpot);
					}
				}
			}

			if (occupied && closestSpot != null) {
				spot = closestSpot;
				break;
			}

			spot = spots.poll();
			if (spot == null) {
				// log("exhausted all options");
				return null;
			}
		}
		ArrayList<int[]> ans = new ArrayList<>();
		while (from[spot[1] * fullMap.length + spot[0]] != -1) {
			ans.add(0, spot);
			int prevSpot = from[spot[1] * fullMap.length + spot[0]];
			spot = new int[] { prevSpot % fullMap.length, (int) (prevSpot / fullMap.length) };
		}
		return ans;
	}

	private boolean spaceIsCootiesFree(int x, int y) {
		for (int[] adj : adjacentSpaces) {
			if (y + adj[1] > -1 && y + adj[1] < robotMap.length && x + adj[0] > -1 && x + adj[0] < robotMap.length) {
				int robotID = robotMap[y+adj[1]][x+adj[0]];
				if (robotID == -1 || robotID == me.id) {
					continue;
				}
				return false;
			}
		}
		return true;
	}

	// mormons may be polygamists but no touchie touchie
	private ArrayList<int[]> bfsCooties(int goalX, int goalY) {
		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		// int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap[0].length];
		for (int i = 0; i < from.length; i++) {
			from[i] = -1;
		}
		// these two are only used if occupied == true
		int[] closestSpot = null;
		int closestDistance = (goalX - me.x) * (goalX - me.x) + (goalY - me.y) * (goalY - me.y);

		while (!(spot[0] == goalX && spot[1] == goalY)) {
			int left = Math.max(0, spot[0] - maxRadius);
			int top = Math.max(0, spot[1] - maxRadius);
			int right = Math.min(fullMap[0].length - 1, spot[0] + maxRadius);
			int bottom = Math.min(fullMap.length - 1, spot[1] + maxRadius);

			for (int x = left; x <= right; x++) {
				int dx = x - spot[0];
				for (int y = top; y <= bottom; y++) {
					int dy = y - spot[1];
					if (dx * dx + dy * dy <= maxRadius * maxRadius && fullMap[y][x] > IMPASSABLE && robotMap[y][x] <= 0
							&& spaceIsCootiesFree(x, y)) {
						if (from[y * fullMap.length + x] != -1) {
							continue;
						}
						int[] newSpot = new int[] { x, y };
						from[y * fullMap.length + x] = spot[1] * fullMap.length + spot[0];

						if (occupied) {
							if ((goalX - x) * (goalX - x) + (goalY - y) * (goalY - y) < closestDistance) {
								closestDistance = (goalX - x) * (goalX - x) + (goalY - y) * (goalY - y);
								closestSpot = newSpot;
								continue;
							}
						}
						spots.add(newSpot);
					}
				}
			}
			if (occupied && closestSpot != null) {
				spot = closestSpot;
				break;
			}
			spot = spots.poll();
			if (spot == null) {
				// log("exhausted all options");
				return null;
			}
		}
		ArrayList<int[]> ans = new ArrayList<>();
		while (from[spot[1] * fullMap.length + spot[0]] != -1) {
			ans.add(0, spot);
			int prevSpot = from[spot[1] * fullMap.length + spot[0]];
			spot = new int[] { prevSpot % fullMap.length, (int) (prevSpot / fullMap.length) };
		}
		return ans;
	}
}