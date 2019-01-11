package bc19;

import java.util.ArrayList;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
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
			HOME = new int[] { me.x, me.y };
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
		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}
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
			for (int dx = -1; dx <= 1; dx++) {
				int testX = me.x + dx;
				if (testX < 0 || testX > fullMap[0].length) {
					continue;
				}
				for (int dy = -1; dy <= 1; dy++) {
					int testY = me.y + dy;
					if (testY < 0 || testY > fullMap.length) {
						continue;
					}
					log("check 1");
					if (robotMap[testY][testX] > 0 && getRobot(robotMap[testY][testX]).unit == SPECS.CASTLE) {
						log("1</pilgrim>");
						return give(dx, dy, me.karbonite, 0);
					}
				}
			}
			log("2</pilgrim>");
			return findBestMove(me.x, me.y, HOME[0], HOME[1], SPECS.PILGRIM);
		}
		log("check 0");
		if (fullMap[me.y][me.x] == KARBONITE) {
			return mine();
		}
		int[] karbLocation = findClosestKarbo(me.x, me.y);
		log("karb found at ("+karbLocation[0]+", "+karbLocation[1]+")");
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

	private int[] findClosestKarbo(int robotX, int robotY) {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = new int[2];
		for (int x = 0; x < fullMap[0].length; x++) {
			for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE && robotMap[y][x] == 0) {
					int dx = x - robotX;
					int dy = y - robotY;
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

	private MoveAction findBestMove(int robotX, int robotY, int goalX, int goalY, int robotType) {
		log("finding moves");
		ArrayList<int[]> possMoves = new ArrayList<>();
		int radius = (int) Math.sqrt(SPECS.UNITS[robotType].SPEED);
		int left = Math.max(0, robotX - radius);
		int top = Math.max(0, robotY - radius);
		int right = Math.min(fullMap[0].length, robotX + radius);
		int bottom = Math.min(fullMap.length, robotY + radius);
		for (int x = left; x <= right; x++) {
			int dx = x - robotX;
			for (int y = top; y <= bottom; y++) {
				int dy = y - robotY;
				if (dx * dx + dy * dy <= radius * radius && fullMap[y][x] > IMPASSABLE && robotMap[y][x] == 0) {
					possMoves.add(new int[] { x, y });
				}
			}
		}
		log("check check");
		int minScore = 64*64;
		for (int i = possMoves.size() - 1; i >= 0; i--) {
			int dx = possMoves.get(i)[0] - goalX;
			int dy = possMoves.get(i)[1] - goalY;
			if (dx * dx + dy * dy > minScore) {
				possMoves.remove(i);
			} else if (dx * dx + dy * dy < minScore) {
				for (int j = possMoves.size() - 1; j > i; j--) {
					possMoves.remove(j);
				}
				minScore = dx * dx + dy * dy;
			}
		}
		if (possMoves.size() == 0) {
			log("wah wah wah");
		}
		int[] randomBest = possMoves.get((int) (Math.random() * possMoves.size()));
		int dx = randomBest[0] - robotX;
		int dy = randomBest[1] - robotY;
		return move(dx, dy);
	}
}
