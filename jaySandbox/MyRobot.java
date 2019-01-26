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
	private int numKarbos = 0;
	private int numFuels = 0;
	private int numCastles = 0;
	private int myMineScore;
	private int[][] allKarbos;
	private int[][] allFuels;
	private int[] fuelMineScores;
	private int currentColonization;
	private boolean[] isMineColonized;
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
			new int[] { -1, -1 }, new int[] { 0, -1 }, new int[] { 1, -1 }, new int[] { 1, 0 }, new int[] { 1, 1 } 
	};

	private ArrayList<int[]> scannedMines;
	private int[][] allMines;
	private ArrayList<ArrayList<int[]>> mineClusters = new ArrayList<>();
	private ArrayList<int[]> mineClusterCenters = new ArrayList<>();
	private ArrayList<int[]> openClusters = new ArrayList<>();
	private ArrayList<int[]> clustersInUse = new ArrayList<>();
	private boolean[] isClusterColonized;
	
	@Override
	public Action turn() {
		if (me.turn == 1) {
			getFullMap();
			getMineSpots();
			//getMineScores();
			fillAllMines();
			idenfityClusters();
			findClusterCenters();
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

			int[] myMine = findClosestCluster();
			for (int i = 0; i < mineClusterCenters.size(); i++) {
				if (mineClusterCenters.get(i) == myMine) {
					// log("found my mine");
					currentColonization = i;
					isClusterColonized[i] = true;
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
			isClusterColonized[castle.castle_talk] = true;
		}
		
		if (numUnits < mineClusters.get(currentColonization).size()) {
			if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
					|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE + 50) {
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
				log("castle is signalling their location at " + (64 * me.y + me.x));
				signal(64 * me.y + me.x, 2);
				log("castle is singalling something else");
				castleTalk(currentColonization);
				return buildUnit(SPECS.PILGRIM, move[0], move[1]);
			}
		}

		if (numUnits >= numMines || fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}

		for (int i = 0; i < mineClusterCenters.size(); i++) {
			if (isClusterColonized[i]) {
				continue;
			}
			for (int[] adj : adjacentSpaces) {
				int freeX = allFuels[i][0] + adj[0];
				int freeY = allFuels[i][1] + adj[1];
				if (freeX <= -1 || freeX >= fullMap[0].length || freeY <= -1 || freeY >= fullMap.length
						|| fullMap[freeY][freeX] != PASSABLE || robotMap[freeY][freeX] > 0) {
					continue;
				}
				currentColonization = i;
				isClusterColonized[i] = true;
				for (int[] move : adjacentSpaces) {
					int buildX = me.x + move[0];
					int buildY = me.y + move[1];
					if (buildX <= -1 || buildX >= fullMap[0].length || buildY <= -1 || buildY >= fullMap.length
							|| fullMap[buildY][buildX] == IMPASSABLE || robotMap[buildY][buildX] > 0) {
						continue;
					}
					numUnits++;
					log("signalling colonization spot at " + (64 * freeY + freeX));
					signal(64 * freeY + freeX, 2);
					log("signalling something else");
					castleTalk(currentColonization);
					return buildUnit(SPECS.PILGRIM, move[0], move[1]);
				}
			}
		}
		return null;
	}

	private Action pilgrimAction() {
		Robot base = null;
		for (int[] move : adjacentSpaces) {
			int testX = me.x + move[0];
			int testY = me.y + move[1];
			if (testX <= -1 || testX >= fullMap[0].length || testY <= -1 || testY >= fullMap.length) {
				continue;
			}
			Robot maybe = getRobot(robotMap[testY][testX]);
			if (robotMap[testY][testX] > 0 && (maybe.unit == SPECS.CASTLE || maybe.unit == SPECS.CHURCH)
					&& maybe.team == me.team) {
				base = maybe;
				karbosInUse.clear();
				fuelsInUse.clear();
				if (me.turn == 1 && isRadioing(base)) {
					HOME = new int[] { base.signal % 64, (int) (base.signal / 64) };
					break;
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
			if (base != null) {
				return give(base.x - me.x, base.y - me.y, me.karbonite, me.fuel);
			}
			log("gary go from (" + me.x + ", " + me.y + ") to (" + HOME[0] + ", " + HOME[1] + ")");
			currentPath = bfs(HOME[0], HOME[1]);
			log("gary found home");
			if (currentPath == null) {
				return null;
			}
			int[] nextMove = currentPath.get(0);
			log("gary baby steps");
			int dx = nextMove[0] - me.x;
			log("gary dxed");
			int dy = nextMove[1] - me.y;
			if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE) {
				currentPath.remove(0);
				log("gary going home");
				return move(dx, dy);
			}
			log("gary no go");
			return null;
		}
		if ((fullMap[me.y][me.x] == KARBONITE || fullMap[me.y][me.x] == FUEL) && robotMap[HOME[1]][HOME[0]] > 0) {
			if (fuel == 0) {
				return null;
			}
			return mine();
		}
		int[] location;
		if (robotMap[HOME[1]][HOME[0]] > 0) {
			/*
			if (20 * numUnits > fuel) {
				location = findClosestFuel();
			} else {
				location = findClosestKarbo();
			}
			*/
			location = findClosestMine();
		}

		else {
			if (Math.abs(HOME[0] - me.x) + Math.abs(HOME[1] - me.y) == 1) {
				log("trying to build a church");
				if (karbonite < SPECS.UNITS[SPECS.CHURCH].CONSTRUCTION_KARBONITE
						|| fuel < SPECS.UNITS[SPECS.CHURCH].CONSTRUCTION_FUEL) {
					return null;
				}
				log("BUILT A CHURCH");
				return buildUnit(SPECS.CHURCH, HOME[0] - me.x, HOME[1] - me.y);
			}
			robotMap[HOME[1]][HOME[0]] = 4096;
			location = HOME;
		}

		if (location == null) {
			location = HOME;
		}

		currentPath = bfs(location[0], location[1]);
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
					numKarbos++;
				} else if (f[i][j]) {
					fullMap[i][j] = FUEL;
					numMines++;
					numFuels++;
				} else {
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	// updates {x, y} of all karbonite and fuel into iterable arraylists
	// call this on turn 1 after getFullMap
	private void getMineSpots() {
		allKarbos = new int[numKarbos][2];
		allFuels = new int[numFuels][2];
		int karboIndex = 0;
		int fuelIndex = 0;
		for (int x = 0; x < fullMap[0].length; x++) {
			for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == KARBONITE) {
					allKarbos[karboIndex][0] = x;
					allKarbos[karboIndex++][1] = y;
				} else if (fullMap[y][x] == FUEL) {
					allFuels[fuelIndex][0] = x;
					allFuels[fuelIndex++][1] = y;
				}
			}
		}
	}

	// call this on turn 1 after getMineSpots
	private void getMineScores() {
		isMineColonized = new boolean[numFuels];
		fuelMineScores = new int[numFuels];
		int index = 0;
		for (int[] fuel : allFuels) {
			int count = 0;
			for (int dx = -2; dx <= 2; dx++) {
				int checkX = fuel[0] + dx;
				if (checkX <= -1 || checkX >= fullMap[0].length) {
					continue;
				}
				for (int dy = -2; dy <= 2; dy++) {
					int checkY = fuel[1] + dy;
					if (checkY <= -1 || checkY >= fullMap.length) {
						continue;
					}
					if (fullMap[checkY][checkX] > PASSABLE) {
						count++;
					}
				}
			}
			fuelMineScores[index++] = count;
		}
	}

	private void fillAllMines()
	{
		allMines = new int[numKarbos + numFuels][];
		int i = 0;
		for (int[] mine : allKarbos)
		{
			allMines[i] = mine;
			i++;
		}
		for (int[] mine : allFuels)
		{
			allMines[i] = mine;
			i++;
		}
	}
	
	private void idenfityClusters()
	{
		scannedMines = new ArrayList<>();
		for (int[] mine : allMines)
		{
			if (scannedMines.contains(mine)) { continue; }
			scannedMines.add(mine);
			ArrayList<int[]> currentCluster = new ArrayList<>();
			mineClusters.add(currentCluster);
			currentCluster.add(mine);
			for (int[] newMine : allMines)
			{
				if (scannedMines.contains(newMine)) { continue; }
				if (tilesInRange(mine, newMine, 50))
				{
					currentCluster.add(newMine);
					scannedMines.add(newMine);
				}
			}
		}
	}

	private void findClusterCenters()
	{
		for (ArrayList<int[]> cluster : mineClusters)
		{
			int clusterSize = cluster.size();
			double cumulativeX = 0.0;
			double cumulativeY = 0.0;
			for (int[] mine : cluster)
			{
				cumulativeX += mine[0];
				cumulativeY += mine[1]; 
			}
			int proposedX = (int) Math.round(cumulativeX/clusterSize);
			int proposedY = (int) Math.round(cumulativeY/clusterSize);
			int[] center = new int[] {proposedX, proposedY};
			mineClusterCenters.add(center);
			openClusters.add(center);
		}
		
		isClusterColonized = new boolean[mineClusterCenters.size()];
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

		for (int[] spot : allKarbos) {
			if (karbosInUse.contains(spot)) {
				if (robotMap[spot[1]][spot[0]] == 0) {
					karbosInUse.remove(spot);
				}
				continue;
			}
			if (robotMap[spot[1]][spot[0]] > 0) {
				karbosInUse.add(spot);
				continue;
			}
			int dx = spot[0] - me.x;
			int dy = spot[1] - me.y;
			if (dx * dx + dy * dy < minDistance) {
				ans = spot;
				minDistance = dx * dx + dy * dy;
			}
		}
		return ans;
	}

	private int[] findClosestFuel() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = null;

		for (int[] spot : allFuels) {
			if (fuelsInUse.contains(spot)) {
				if (robotMap[spot[1]][spot[0]] == 0) {
					fuelsInUse.remove(spot);
				}
				continue;
			}
			if (robotMap[spot[1]][spot[0]] > 0) {
				fuelsInUse.add(spot);
				continue;
			}
			int dx = spot[0] - me.x;
			int dy = spot[1] - me.y;
			if (dx * dx + dy * dy < minDistance) {
				ans = spot;
				minDistance = dx * dx + dy * dy;
			}
		}
		return ans;
	}

	private int[] findClosestMine() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = null;

		for (int[] spot : allFuels) {
			if (fuelsInUse.contains(spot)) {
				if (robotMap[spot[1]][spot[0]] == 0) {
					fuelsInUse.remove(spot);
				}
				continue;
			}
			if (robotMap[spot[1]][spot[0]] > 0) {
				fuelsInUse.add(spot);
				continue;
			}
			int dx = spot[0] - me.x;
			int dy = spot[1] - me.y;
			if (dx * dx + dy * dy < minDistance) {
				ans = spot;
				minDistance = dx * dx + dy * dy;
			}
		}
		for (int[] spot : allKarbos) {
			if (karbosInUse.contains(spot)) {
				if (robotMap[spot[1]][spot[0]] == 0) {
					karbosInUse.remove(spot);
				}
				continue;
			}
			if (robotMap[spot[1]][spot[0]] > 0) {
				karbosInUse.add(spot);
				continue;
			}
			int dx = spot[0] - me.x;
			int dy = spot[1] - me.y;
			if (dx * dx + dy * dy < minDistance) {
				ans = spot;
				minDistance = dx * dx + dy * dy;
			}
		}
		return ans;
	}

	private int[] findClosestCluster()
	{
		int minDistance = fullMap.length * fullMap.length;
		int[] ans = null;

		for (int[] spot : mineClusterCenters) {
			if (clustersInUse.contains(spot)) {
				continue;
			}
			int dx = spot[0] - me.x;
			int dy = spot[1] - me.y;
			if (dx * dx + dy * dy < minDistance) {
				ans = spot;
				minDistance = dx * dx + dy * dy;
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
				log("BFS exhausted all options");
				return null;
			}
		}
		ArrayList<int[]> ans = new ArrayList<>();
		while (from[spot[1] * fullMap.length + spot[0]] != -1) {
			ans.add(0, spot);
			int prevSpot = from[spot[1] * fullMap.length + spot[0]];
			spot = new int[] { prevSpot % fullMap.length, (int) (prevSpot / fullMap.length) };
		}
		log("BFS returned " + ans.size() + "-step path");
		return ans;
	}

	private boolean spaceIsCootiesFree(int x, int y) {
		for (int[] adj : adjacentSpaces) {
			if (y + adj[1] > -1 && y + adj[1] < robotMap.length && x + adj[0] > -1 && x + adj[0] < robotMap.length) {
				int robotID = robotMap[y + adj[1]][x + adj[0]];
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

	private boolean tilesInRange(int[] tile1, int[] tile2, int rangeSquared)
	{
		return ((tile1[0]- tile2[0])*(tile1[0]- tile2[0]) + (tile1[1]- tile2[1])*(tile1[1]- tile2[1]) <= rangeSquared);
	}
}