package bc19;

import java.util.ArrayList;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	private int numOfUnits = 0;
	private int[][] fullMap;
	private int[][] robotMap;
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private int[] HOME;

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
	}

	private Action castleAction() {
		castleTalk(0);
		for (Robot robot : getVisibleRobots()) {
			if (robot.unit == SPECS.CASTLE) {
				log("castle!!!!");
				if (robot.id == me.id) {
					continue;
				}
				log("found castle " + robot.id);
				numOfUnits += robot.castle_talk;
			}
		}
		log("POPULATION: " + numOfUnits);
		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}
		int buildX, buildY;
		boolean haveNeighbors = false;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) {
					dy++;
				}
				buildX = me.x + dx;
				buildY = me.y + dy;
				if (buildX > -1 && buildX < fullMap[0].length && buildY > -1 && buildY < fullMap.length
						&& fullMap[buildY][buildX] > IMPASSABLE) {
					if (robotMap[buildY][buildX] > 0) {
						haveNeighbors = true;
						continue;
					}
					numOfUnits++;
					signal(numOfUnits, 2);
					castleTalk(1);
					return buildUnit(SPECS.PILGRIM, dx, dy);
				}
			}
		}
		if (haveNeighbors) {
			signal(numOfUnits, 2);
		}
		return null;
	}

	private Action pilgrimAction() {
		Robot castle = null;
		for (int dx = -1; dx <= 1; dx++) {
			int testX = me.x + dx;
			if (testX <= -1 || testX >= fullMap[0].length) {
				continue;
			}
			for (int dy = -1; dy <= 1; dy++) {
				int testY = me.y + dy;
				if (testY <= -1 || testY >= fullMap.length) {
					continue;
				}
				if (robotMap[testY][testX] > 0 && getRobot(robotMap[testY][testX]).unit == SPECS.CASTLE) {
					castle = getRobot(robotMap[testY][testX]);
					if (me.turn == 1) {
						HOME = new int[] { castle.x, castle.y };
					}
					if (isRadioing(castle)) {
						numOfUnits = castle.signal;
					}
				}
			}
		}
		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY
				|| me.fuel == SPECS.UNITS[SPECS.PILGRIM].FUEL_CAPACITY) {
			if (castle != null) {
				return give(castle.x - me.x, castle.y - me.y, me.karbonite, me.fuel);
			}
			return findBestMove(HOME[0], HOME[1], true);
		}
		if (fullMap[me.y][me.x] == KARBONITE || fullMap[me.y][me.x] == FUEL) {
			return mine();
		}
		int[] location;
		if (10 * numOfUnits > fuel) {
			location = findClosestFuel();
		} else {
			location = findClosestKarbo();
		}
		return findBestMove(location[0], location[1], true);
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

	private int[] findClosestKarbo() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = new int[] { 0, 0 };
		for (int x = 0; x < fullMap[0].length; x++) {
			for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE && robotMap[y][x] <= 0) {
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans[0] = x;
						ans[1] = y;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}

	private int[] findClosestFuel() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = new int[] { 0, 0 };
		for (int x = 0; x < fullMap[0].length; x++) {
			for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == FUEL && robotMap[y][x] <= 0) {
					int dx = x - me.x;
					int dy = y - me.y;
					if (dx * dx + dy * dy < minDistance) {
						ans[0] = x;
						ans[1] = y;
						minDistance = dx * dx + dy * dy;
					}
				}
			}
		}
		return ans;
	}

	private Action findBestMove(int goalX, int goalY, boolean fuelEfficient) {
		ArrayList<int[]> possMoves = new ArrayList<>();
		int radius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		int left = Math.max(0, me.x - radius);
		int top = Math.max(0, me.y - radius);
		int right = Math.min(fullMap[0].length - 1, me.x + radius);
		int bottom = Math.min(fullMap.length - 1, me.y + radius);
		for (int x = left; x <= right; x++) {
			int dx = x - me.x;
			for (int y = top; y <= bottom; y++) {
				int dy = y - me.y;
				if (dx * dx + dy * dy <= radius * radius && fullMap[y][x] > IMPASSABLE) {
					if (robotMap[y][x] <= 0 || dx * dx + dy * dy == 1 && (me.karbonite > 0 || me.fuel > 0)) {
						possMoves.add(new int[] { x, y });
					}
				}
			}
		}
		if (fuelEfficient) {
			if (fuel == 0) {
				return null;
			}
			int olddx = me.x - goalX;
			int olddy = me.y - goalY;
			for (int i = possMoves.size() - 1; i >= 0; i--) {
				int x = possMoves.get(i)[0];
				int y = possMoves.get(i)[1];
				int newdx = x - goalX;
				int newdy = y - goalY;
				if ((x - me.x) * (x - me.x) + (y - me.y) * (y - me.y) != 1
						|| newdx * newdx + newdy * newdy > olddx * olddx + olddy * olddy) {
					possMoves.remove(i);
				}
			}
			if (possMoves.size() == 0) {
				return findBestMove(goalX, goalY, false);
			}
		} else {
			int minScore = 64 * 64;
			for (int i = possMoves.size() - 1; i >= 0; i--) {
				int dx = possMoves.get(i)[0] - goalX;
				int dy = possMoves.get(i)[1] - goalY;
				if (dx * dx + dy * dy > minScore || SPECS.UNITS[me.unit].FUEL_PER_MOVE * (dx * dx + dy * dy) > fuel) {
					possMoves.remove(i);
				} else if (dx * dx + dy * dy < minScore) {
					for (int j = possMoves.size() - 1; j > i; j--) {
						possMoves.remove(j);
					}
					minScore = dx * dx + dy * dy;
				}
			}
		}
		if (possMoves.size() == 0) {
			return null;
		}
		int[] randomBest = possMoves.get((int) (Math.random() * possMoves.size()));
		if (robotMap[randomBest[1]][randomBest[0]] > 0) {
			return give(randomBest[0] - me.x, randomBest[1] - me.y, me.karbonite, me.fuel);
		}
		return move(randomBest[0] - me.x, randomBest[1] - me.y);
	}
}
