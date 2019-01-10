package bc19;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	private int[][] fullMap;
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private int[] HOME;

	@Override
	public Action turn() {
		if (me.turn == 1) {
			HOME = new int[] { me.x, me.y };
			getFullMap();
		}
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
	}

	private Action castleAction() {
		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}
		int[][] robotMap = getVisibleRobotMap();
		int buildX, buildY;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					dy++;
				}
				buildX = me.x + dx;
				buildY = me.y + dy;
				if (buildX > -1 && buildX < fullMap[0].length && buildY > -1 && buildY < fullMap.length
						&& fullMap[buildY][buildX] > IMPASSABLE && robotMap[buildY][buildX] == 0) {
					return buildUnit(SPECS.PILGRIM, dx, dy);
				}
			}
		}
		return null;
	}

	private Action pilgrimAction() {
		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY) {
			int[][] robotMap = getVisibleRobotMap();
			for (int dx = -1; dx >= 1; dx++) {
				int testX = me.x + dx;
				if (testX < 0 || testX > fullMap[0].length) {
					continue;
				}
				for (int dy = -1; dy >= 1; dy++) {
					int testY = me.y + dy;
					if (testY < 0 || testY > fullMap.length) {
						continue;
					}
					if (getRobot(robotMap[testY][testX]).unit == SPECS.CASTLE) {
						return give(dx, dy, me.karbonite, 0);
					}
				}
			}
			return findBestMove(me.x, me.y, HOME[0], HOME[1], SPECS.PILGRIM);
		}

		if (fullMap[me.y][me.x] == KARBONITE) {
			return mine();
		}
		int[] karbLocation = findClosestFreeKarbonite(me.x, me.y);
		if (karbLocation == null) {
			log("no karbo on map?");
			return null;
		}
		return findBestMove(me.x, me.y, karbLocation[0], karbLocation[1], SPECS.PILGRIM);
	}

	// makes fullMap
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
				} else if (f[i][j]) {
					fullMap[i][j] = FUEL;
				} else {
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	private int[] findClosestFreeKarbonite(int robotX, int robotY) {
		int[][] robotMap;
		for (int radius = 0; radius < fullMap.length; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int testX = me.x + dx;
				if (testX < 0 || testX > fullMap[0].length) {
					continue;
				}
				int dy = (int) Math.sqrt(radius * radius - dx * dx);
				int testY = me.y + dy;
				if (testY < 0 || testY > fullMap.length) {
					continue;
				}
				if (fullMap[testY][testX] == KARBONITE) {
					robotMap = (robotMap == null ? getVisibleRobotMap() : robotMap);
					if (robotMap[testY][testX] == 0) {
						return new int[] { testX, testY };
					}
				}
				testY = me.y - dy;
				if (testY < 0 || testY > fullMap.length) {
					continue;
				}
				if (fullMap[testY][testX] == KARBONITE) {
					robotMap = (robotMap == null ? getVisibleRobotMap() : robotMap);
					if (robotMap[testY][testX] == 0) {
						return new int[] { testX, testY };
					}
				}
			}
		}
	}

	private MoveAction findBestMove(int robotX, int robotY, int goalX, int goalY, int robotType) {
		int dirX = goalX - robotX;
		int dirY = goalY - robotY;
		int maxR = SPECS.UNITS[robotType].SPEED;
		int[][] others = getVisibleRobotMap();
		// goal: try dirX dirY, adjust slightly left, adjust slightly right, etc.
		int magnitude = dirX * dirX + dirY * dirY;
		if (magnitude > maxR) {
			dirX /= magnitude;
			dirY /= magnitude;
		}
		magnitude = dirX * dirX + dirY * dirY;
		int dx, dy;
		findMoves: for (int radiusSqrd = magnitude; radiusSqrd > 0; radiusSqrd--) {
			for (int offset = 0;; offset++) {
				if (dirX + offset > maxR && dirX - offset < -maxR) {
					continue findMoves;
				}
				dx = dirX + offset;

				if (dx <= maxR && me.x + dx > -1 && me.x + dx < fullMap[0].length) {
					
					dy = (int) Math.sqrt(radiusSqrd - dx * dx);
					if (me.y + dy > -1 && me.y + dy < fullMap.length && fullMap[me.y + dy][me.x + dx] > IMPASSABLE
							&& others[me.y + dy][me.x + dx] == 0) {
						log("found a move!");
						return move(dx, dy);
					}
					dy = -dy;
					if (me.y + dy > -1 && me.y + dy < fullMap.length && fullMap[me.y + dy][me.x + dx] > IMPASSABLE
							&& others[me.y + dy][me.x + dx] == 0) {
						log("found a move-!");
						return move(dx, dy);
					}
				}

				dx = dirX - offset;

				if (dx >= -maxR && me.x + dx > -1 && me.x + dx < fullMap[0].length) {
					
					dy = (int) Math.sqrt(radiusSqrd - dx * dx);
					if (me.y + dy > -1 && me.y + dy < fullMap.length && fullMap[me.y + dy][me.x + dx] > IMPASSABLE
							&& others[me.y + dy][me.x + dx] == 0) {
						log("found a -move!");
						return move(dx, dy);
					}
					dy = -dy;
					if (me.y + dy > -1 && me.y + dy < fullMap.length && fullMap[me.y + dy][me.x + dx] > IMPASSABLE
							&& others[me.y + dy][me.x + dx] == 0) {
						log("found a -move-!");
						return move(dx, dy);
					}
				}
			}
		}
		log("nada");
		return null;
	}
}
