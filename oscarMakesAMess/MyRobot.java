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
		log("<pilgrim>");
		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY) {
			log("check 00");
			int[][] robotMap = getVisibleRobotMap();
			for (int dx = -1; dx <= 1; dx++) {
				int testX = me.x + dx;
				if (testX < 0 || testX > fullMap[0].length) {
					continue;
				}
				log("check 0");
				for (int dy = -1; dy <= 1; dy++) {
					int testY = me.y + dy;
					if (testY < 0 || testY > fullMap.length) {
						continue;
					}
					log("check 1");
					if (getRobot(robotMap[testY][testX]).unit == SPECS.CASTLE) {
						log("1</pilgrim>");
						return give(dx, dy, me.karbonite, 0);
					}
				}
			}
			log("2</pilgrim>");
			return findBestMove(me.x, me.y, HOME[0], HOME[1], SPECS.PILGRIM);
		}

		if (fullMap[me.y][me.x] == KARBONITE) {
			log("check 2.5");
			return mine();
		}
		int[] karbLocation = findClosestFreeKarbonite(me.x, me.y);
		if (karbLocation == null) {
			return null;
		}
		log("3</pilgrim>");
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
		int[][] robotMap = getVisibleRobotMap();
		for (int radius2 = 0; radius2 < fullMap.length * fullMap.length; radius2++) {
			int limit = (int) Math.sqrt(radius2);
			for (int dx = 0; dx <= limit; dx++) {
				int testX = me.x + dx;
				if (testX > -1 && testX < fullMap[0].length) {
					int dy = (int) Math.sqrt(radius2 - dx * dx);
					int testY = me.y + dy;
					if (testY > -1 && testY < fullMap.length) {
						if (fullMap[testY][testX] == KARBONITE) {
							if (robotMap[testY][testX] == 0 || (testY == me.y && testX == me.x)) {
								return new int[] { testX, testY };
							}
						}
					}
					
					testY = me.y - dy;
					if (testY > -1 && testY < fullMap.length) {
						if (fullMap[testY][testX] == KARBONITE) {
							if (robotMap[testY][testX] == 0 || (testY == me.y && testX == me.x)) {
								return new int[] { testX, testY };
							}
						}
					}
				}

				testX = me.x - dx;
				if (testX > -1 && testX < fullMap[0].length) {
					int dy = (int) Math.sqrt(radius2 - dx * dx);
					int testY = me.y + dy;
					if (testY > -1 && testY < fullMap.length) {
						if (fullMap[testY][testX] == KARBONITE) {
							if (robotMap[testY][testX] == 0 || (testY == me.y && testX == me.x)) {
								return new int[] { testX, testY };
							}
						}
					}
					
					testY = me.y - dy;
					if (testY > -1 && testY < fullMap.length) {
						if (fullMap[testY][testX] == KARBONITE) {
							if (robotMap[testY][testX] == 0 || (testY == me.y && testX == me.x)) {
								return new int[] { testX, testY };
							}
						}
					}
				}
			}
		}
		return null;
	}

	private MoveAction findBestMove(int robotX, int robotY, int goalX, int goalY, int robotType) {
		int dirX = goalX - robotX;
		int dirY = goalY - robotY;
		int maxR = SPECS.UNITS[robotType].SPEED;
		int[][] others = getVisibleRobotMap();
		// goal: try dirX dirY, adjust slightly left, adjust slightly right, etc.
		int magnitude = dirX * dirX + dirY * dirY;
		if (magnitude > maxR) {
			dirX = (int) (dirX / magnitude);
			dirY = (int) (dirY / magnitude);
			magnitude = dirX * dirX + dirY * dirY;
		}
		int dx, dy;
		findMoves: for (int radiusSqrd = magnitude; radiusSqrd > 0; radiusSqrd--) {
			log("radius: "+radiusSqrd);
			for (int offset = 0;; offset++) {
				dx = dirX + offset;

				if (dx * dx <= maxR && me.x + dx > -1 && me.x + dx < fullMap[0].length) {

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
				if (dx * dx > maxR) {
					continue findMoves;
				}
				if (me.x + dx > -1 && me.x + dx < fullMap[0].length) {
					
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
