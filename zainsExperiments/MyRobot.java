package bc19;

import bc19.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class MyRobot extends BCAbstractRobot {
	// important
	private final int IMPASSABLE = -1, PASSABLE = 0, KARBONITE = 1, FUEL = 2;
	private boolean hRefl;
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int[][] robotMap;
	private int xorKey; // XOR any signal by this, and any castletalk by this % 256
	// Note: the encodedCastleLocs are sort of separate and thus XOR'd with this % 256
	// separately; don't worry 'bout it.
	private final int[][] adjacentSpaces = new int[][] { // Matrix of adjacent spaces, relative to the Robot
		new int[] {0,1},
		new int[] {-1,1},
		new int[] {-1,0},
		new int[] {-1,-1},
		new int[] {0,-1},
		new int[] {1,-1},
		new int[] {1,0},
		new int[] {1,1}
	};
	private int numCastles; // DO NOT USE for castles. Use robs[0].size() instead.
	private int ourDeadCastles = 0;
	private int[][] castleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[][] enemyCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int globalTurn;
	private int numMines = 0;

	// for castles
	private ArrayList<Integer>[] robs = new ArrayList[6];

	// For colonization
	private int myMineScore;
	private int[][] allKarbos;
	private int[][] allFuels;
	private int[] allMineScores;
	private int currentColonization;
	private boolean[] isMineColonized;
	private final int mineClusterRadiusSqrd = 25;
	private ArrayList<int[]> mineClusterCenters;
	private ArrayList<ArrayList<int[]>> mineClusters;
	private int[][] allMines;
	private int numKarbos = 0;
	private int numFuels = 0;
	private int numLocalPilgs = 0;

	// For pilgrims
	private ArrayList<int[]> karbosInUse = new ArrayList<>(); // logs karbos and fuels that other robots are on
	private ArrayList<int[]> fuelsInUse = new ArrayList<>(); // you should clear these whenever the unit returns to a castle
	private int[] HOME;

	// For pathing
	private ArrayList<int[]> currentPath = null;
	private int locInPath;

	// For lattice
	private int castleDir;
	private int sideDir;
	private boolean arrived;

	// For attacking
	private final int[] attackPriority = new int[] {4, 5, 3, 0, 2, 1};
	private int targetCastle;


	public Action turn() {
		if (me.turn == 1) {
			for(int i = 0; i < 6; i++)
			{
				robs[i] = new ArrayList<Integer>();
			}
			getFMap();
			hRefl = getReflDir();
			getMineSpots();
			setXorKey();
			castleTalk(me.unit);
		}
		else
		{
			globalTurn += 1;
		}
		robotMap = getVisibleRobotMap();
		switch (me.unit) {
		case 0:
			return castle();
		case 1:
			return church();
		case 2:
			return pilgrim();
		case 3:
			return crusader();
		case 4:
			return prophet();
		case 5:
			return preacher();
		}
		return null;
	}

	private Action castle()
	{
		if (me.turn == 1)
		{
			globalTurn = 1;
			castleLocs[0] = new int[] {me.x, me.y};
			robs[0].add(me.id);
			updateRobs();

			fillAllMines();
			getMineScores();
			identifyClusters();
			findClusterCenters();

			int[] myMine = findClosestMine();
			for (int i = 0; i < mineClusters.size(); i++) {
				if (mineClusters.get(i).contains(myMine)) {
					myMineScore = mineClusters.get(i).size();
					currentColonization = i;
					isMineColonized[i] = true;
				}
			}

			return null; //REMOVE THIS HERE
		}

		else if (me.turn == 2)
		{	
			if (robs[0].size() > 1)
			{
				castleTalk(me.x ^ (xorKey % 256));

				for(int i = 1; i < robs[0].size(); i++)
				{
					Robot cast = getRobot(robs[0].get(i));
					if(cast.turn == 1)
					{
						castleLocs[i][0] = cast.castle_talk ^ (xorKey % 256);
					}
				}
			}
		}
		else if(me.turn == 3)
		{	
			if (robs[0].size() > 1)
			{
				castleTalk(me.y ^ (xorKey % 256));

				for(int i = 1; i < robs[0].size(); i++)
				{
					Robot cast = getRobot(robs[0].get(i));
					if(cast.turn == 2)
					{
						castleLocs[i][1] = cast.castle_talk ^ (xorKey % 256);
					}
					else
					{
						castleLocs[i][0] = cast.castle_talk ^ (xorKey % 256);
					}
				}
			}
		}
		else if(me.turn == 4)
		{
			if (robs[0].size() > 1)
			{
				for(int i = 1; i < robs[0].size(); i++)
				{
					Robot cast = getRobot(robs[0].get(i));
					if(cast.turn == 2)
					{
						castleLocs[i][1] = cast.castle_talk ^ (xorKey % 256);
					}
				}
			}
			getEnemyCastleLocs();
		}

		else if (me.turn == 849)
		{
			int numBuild = robs[0].size() + robs[1].size();

			if(numCastles == 1)
			{
				signal(4096, (int) Math.floor(fullMap.length * fullMap.length / numBuild / numBuild * 2));
			}
			else
			{
				signal(castleLocs[1][0] + castleLocs[1][1] * 64,  (int) Math.floor(fullMap.length * fullMap.length / numBuild / numBuild * 2));
			}
		}
		else if (me.turn == 850)
		{
			int numBuild = robs[0].size() + robs[1].size();

			if(numCastles <= 2)
			{
				signal(4096,  (int) Math.floor(fullMap.length * fullMap.length / numBuild / numBuild * 2));
			}
			else
			{
				signal(castleLocs[2][0] + castleLocs[2][1] * 64,  (int) Math.floor(fullMap.length * fullMap.length / numBuild / numBuild * 2));
			}
		}


		// Every turn

		updateRobs();

		for(int i = 0; i < robs[0].size(); i++)
		{
			isMineColonized[getCastObj(i).castle_talk - 1] = true;
		}

		// Just a log
		if(me.turn % 20 == 0)
		{
			log("Turn: " + me.turn + ". Pilgrim population: " + robs[2].size() + ". Prophet population:  " + robs[4].size() + ". Pilgrim limit: " + numMines + ".");
		}

		// Defend if under attack
		int[] atk = autoAttack();
		if(atk != null)
		{
			int[] loc = availAdjSq(new int[] {atk[0] > 0 ? 1 : (atk[0] < 0 ? -1 : 0), atk[1] > 0 ? 1 : (atk[1] < 0 ? -1 : 0)});

			if(karbonite >= 30 && fuel >= 50 && getRobot(robotMap[me.y + atk[1]][me.x + atk[0]]).unit != SPECS.PILGRIM && loc != null)
			{
				return buildUnit(3, loc[0], loc[1]);
			}

			return attack(atk[0], atk[1]);
		}

		if (numLocalPilgs < myMineScore) {
			if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
					|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
				return null;
			}
			int[] loc = randomAdjSq();
			if(loc != null)
			{
				numLocalPilgs += 1;
				signal(64 * me.y + me.x, 2);
				castleTalk(currentColonization + 1);
				return buildUnit(SPECS.PILGRIM, loc[0], loc[1]);
			}
			return null;
		}

		if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
				|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE) {
			return null;
		}

		for (int i = 0; i < mineClusterCenters.size(); i++) {
			if (isMineColonized[i]) {
				continue;
			}
			int[] center = mineClusterCenters.get(i);
			currentColonization = i;
			isMineColonized[i] = true;
			for (int[] move : adjacentSpaces) {
				int buildX = me.x + move[0];
				int buildY = me.y + move[1];
				if (buildX <= -1 || buildX >= fullMap[0].length || buildY <= -1 || buildY >= fullMap.length
						|| fullMap[buildY][buildX] == IMPASSABLE || robotMap[buildY][buildX] > 0) {
					continue;
				}
				signal(64 * center[1] + center[0], 2);
				castleTalk(currentColonization + 1);
				return buildUnit(SPECS.PILGRIM, move[0], move[1]);
			}
		}
		/*
		// If there's enough pilgrims and some extra fuel (enough for all pilgrims to move max distance 1.5 times), build a prophet.

			if(me.turn < 850 && fuel >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_FUEL + 2 + robs[2].size() * 6 && karbonite >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_KARBONITE)
			{
				int doit; // If there's lots of resources, definitely build. If only a little, maybe build one.
				// This is so all castles and churches build about the same amount.
				if(fuel >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_FUEL * (robs[0].size() + robs[1].size()) + 2 + robs[2].size() * 6 && karbonite >= SPECS.UNITS[SPECS.PROPHET].CONSTRUCTION_KARBONITE * robs[0].size())
				{
					doit = 0;
				}
				else
				{
					doit = (int) (Math.random() * (robs[0].size() + robs[1].size()));
				}

				if(doit == 0)
				{
					int[] loc = randomAdjSq();
					if(loc != null)
					{
						return buildUnit(4, loc[0], loc[1]);
					}
				}
			}
		 */

		return null; //default
	}

	private Action church() 
	{
		if (me.turn == 1)
		{
			robs[0].add(me.id);

			for (Robot cast : getVisibleRobots()) {
				if (cast.team == me.team && cast.id != me.id) {
					robs[0].add(cast.id);
				}
			}

			fillAllMines();
			getMineScores();
			identifyClusters();
			numLocalPilgs = 1;
			castleLocs[0] = new int[] {me.x, me.y};
			int[] myMine = findClosestMine();
			for (ArrayList<int[]> cluster : mineClusters)
			{
				if (cluster.contains(myMine))
				{
					myMineScore = cluster.size();
				}
			}
			log("church is awake with "+myMineScore+" mines");
		}
		if (numLocalPilgs < myMineScore)
		{
			log("trying to build pilgrim");
			if (fuel < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_FUEL + 2
					|| karbonite < SPECS.UNITS[SPECS.PILGRIM].CONSTRUCTION_KARBONITE)
			{
				return null;
			}
			for (int[] move : adjacentSpaces)
			{
				int buildX = me.x + move[0];
				int buildY = me.y + move[1];
				if (buildX <= -1 || buildX >= fullMap[0].length || buildY <= -1 || buildY >= fullMap.length
						|| fullMap[buildY][buildX] == IMPASSABLE || robotMap[buildY][buildX] > 0)
				{
					continue;
				}
				numLocalPilgs++;
				log("church is signalling their location at " + (64 * me.y + me.x));
				signal(64 * me.y + me.x, 2);
				return buildUnit(SPECS.PILGRIM, move[0], move[1]);
			}
		}
		return null;
	}

	private Action pilgrim()
	{
		if (me.turn == 1)
		{
			getHomeCastle();
			getEnemyCastleLocs();
		}

		Robot base = null;
		for (int[] move : adjacentSpaces)
		{
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
			}
		}

		if (currentPath != null && currentPath.size() > 0)
		{
			int[] nextMove = currentPath.get(0);
			int dx = nextMove[0] - me.x;
			int dy = nextMove[1] - me.y;
			if (robotMap[nextMove[1]][nextMove[0]] <= 0)
			{
				if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE)
				{
					currentPath.remove(0);
					return move(dx, dy);
				}
			}
		}

		if (me.karbonite == SPECS.UNITS[SPECS.PILGRIM].KARBONITE_CAPACITY || me.fuel == SPECS.UNITS[SPECS.PILGRIM].FUEL_CAPACITY)
		{
			if (base != null)
			{
				return give(base.x - me.x, base.y - me.y, me.karbonite, me.fuel);
			}
			currentPath = bfs(HOME[0], HOME[1]);
			if (currentPath == null)
			{
				log("gary no found home");
				return null;
			}
			int[] nextMove = currentPath.get(0);
			int dx = nextMove[0] - me.x;
			int dy = nextMove[1] - me.y;
			if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE)
			{
				currentPath.remove(0);
				return move(dx, dy);
			}
			return null;
		}
		if ((fullMap[me.y][me.x] == KARBONITE || fullMap[me.y][me.x] == FUEL) && robotMap[HOME[1]][HOME[0]] > 0)
		{
			if (fuel == 0) {
				return null;
			}
			return mine();
		}
		int[] location;
		if (robotMap[HOME[1]][HOME[0]] > 0)
		{
			location = findClosestMine();
			if (!tilesInRange(location, HOME, mineClusterRadiusSqrd))
			{
				log("robot is straying away from base");
			}
		}

		else
		{
			if (Math.abs(HOME[0] - me.x) + Math.abs(HOME[1] - me.y) == 1)
			{
				if (karbonite < SPECS.UNITS[SPECS.CHURCH].CONSTRUCTION_KARBONITE
						|| fuel < SPECS.UNITS[SPECS.CHURCH].CONSTRUCTION_FUEL)
				{
					return null;
				}
				return buildUnit(SPECS.CHURCH, HOME[0] - me.x, HOME[1] - me.y);
			}
			robotMap[HOME[1]][HOME[0]] = 4096;
			location = HOME;
		}

		if (location == null)
		{
			location = HOME;
		}

		currentPath = bfs(location[0], location[1]);
		if (currentPath == null)
		{
			return null;
		}
		int[] nextMove = currentPath.get(0);
		int dx = nextMove[0] - me.x;
		int dy = nextMove[1] - me.y;
		if (fuel >= (dx * dx + dy * dy) * SPECS.UNITS[SPECS.PILGRIM].FUEL_PER_MOVE)
		{
			currentPath.remove(0);
			return move(dx, dy);
		}
		
		return null;
	}

	private Action crusader()
	{

		if (me.turn == 1)
		{
			getHomeCastle();
			getCastleDir();
			if(castleDir % 2 == 0)
			{
				sideDir = (((int) (Math.random() * 2)) * 4 + castleDir + 2) % 8;
			}
		}

		int[] atk = autoAttack();
		if(atk != null)
		{
			if(fuel >= 10)
			{
				return attack(atk[0], atk[1]);
			}
			else
			{
				return null;
			}
		}

		getCastleLocs(); // MAKE THIS WORK LIASFUGHASRGUIHARGLASIGHASGHALSUHGUASH GASDUGH ASDLHGKHAJKSDHGJKAHSDLUGHASDJGNSAD.NGAKJSG

		if(globalTurn >= 850)
		{
			updateTargetCastle();

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0 || globalTurn == 850)
			{
				currentPath = bfs(enemyCastleLocs[targetCastle][0], enemyCastleLocs[targetCastle][1]);
			}

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				log("Prophet BFS returned null (or something invalid). Turn: " + globalTurn);
				if(fuel >= numMines * 2) // leave fuel for mining
				{
					int[] mov = randomAdjSq();

					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}

					return null;
				}
				else
				{
					return null;
				}
			}

			int[] mov = new int[] {currentPath.get(locInPath)[0] - me.x, currentPath.get(locInPath)[1] - me.y};

			if(fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + numMines * .7)
			{
				locInPath += 1;
				return move(mov[0], mov[1]);
			}
			else
			{	
				return null;
			}
		}
		else
		{
			if(fuel >= numMines * 2)
			{
				if(moveAway())
				{
					int[] mov = exploreLattice();
					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}
				}

			}
		}
		return null;
	}

	private Action prophet()
	{
		if (me.turn == 1)
		{
			getHomeCastle();
			arrived = false;
		}

		getCastleLocs(); // Only does anything on turns 849-850

		int[] atk = autoAttack();
		if(atk != null)
		{
			return attack(atk[0], atk[1]);
		}

		if(globalTurn >= 850)
		{
			updateTargetCastle();

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0 || globalTurn == 850)
			{
				currentPath = bfs(enemyCastleLocs[targetCastle][0], enemyCastleLocs[targetCastle][1]);
			}

			if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
			{
				log("Prophet BFS returned null (or something invalid). Turn: " + globalTurn);
				if(fuel >= numMines * 2) // leave fuel for mining
				{
					int[] mov = randomAdjSq();

					if(mov != null)
					{
						return move(mov[0], mov[1]);
					}

					return null;
				}
				else
				{
					return null;
				}
			}

			int[] mov = new int[] {currentPath.get(locInPath)[0] - me.x, currentPath.get(locInPath)[1] - me.y};

			if(fuel >= (mov[0] * mov[0] + mov[1] * mov[1]) * 2 + numMines * .7)
			{
				locInPath += 1;
				return move(mov[0], mov[1]);
			}
			else
			{	
				return null;
			}
		}
		else
		{
			if(!arrived && fuel >= numMines * 2)
			{
				if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
				{
					currentPath = goToLattice();
				}

				if (currentPath == null || currentPath.size() <= locInPath || robotMap[currentPath.get(locInPath)[1]][currentPath.get(locInPath)[0]] > 0)
				{
					log("Prophet BFS returned null (or something invalid). Turn: " + globalTurn);

					int[] mov = randomAdjSq();
					if(mov != null)
					{
						if((me.x + mov[0] + me.y + mov[1]) % 2 == 0)
						{
							arrived = true;
						}
						return move(mov[0], mov[1]);
					}
					return null;
				}

				int[] mov = new int[] {currentPath.get(locInPath)[0] - me.x, currentPath.get(locInPath)[1] - me.y};
				locInPath += 1;
				if((me.x + mov[0] + me.y + mov[1]) % 2 == 0 && !isNextToHome(me.x + mov[0], me.y + mov[1]))
				{
					arrived = true;
				}
				return move(mov[0], mov[1]);
			}
		}
		return null;
	}

	private Action preacher()
	{
		if (me.turn == 1)
		{
			getCastleDir();
			getHomeCastle();
			if(castleDir % 2 == 0)
			{
				sideDir = (((int) (Math.random() * 2)) * 4 + castleDir + 2) % 8;
			}
		}

		AttackAction atk = preacherAttack();
		if(atk != null)
		{
			return atk;
		}

		getCastleLocs();

		return null;
	}

	private void getFMap() // makes fullMap
	{
		boolean[][] m = getPassableMap();
		boolean[][] k = getKarboniteMap();
		boolean[][] f = getFuelMap();

		fullMap = new int[m.length][m.length];

		int h = m.length;
		int w = h;

		for (int i = 0; i < h; i++) {
			for (int j = 0; j < w; j++) {
				if (!m[i][j]) {
					fullMap[i][j] = IMPASSABLE;
				} else if (k[i][j]) {
					numKarbos++;
					fullMap[i][j] = KARBONITE;
				} else if (f[i][j]) {
					numFuels++;
					fullMap[i][j] = FUEL;
				} else {
					fullMap[i][j] = PASSABLE;
				}
			}
		}

		numMines = numKarbos + numFuels;
	}

	private boolean getReflDir() // set hRefl
	{
		int top = (fullMap.length + 1) / 2;
		int left = (fullMap[0].length + 1) / 2;

		for (int i = 0; i < top; i++) // Goes through top left quarter of map and tests one cell at a time
		{ // for whether it's reflected horizontally then vertically.
			for (int j = 0; j < left; j++) // If a discrepancy is found, method returns.
			{
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		for (int i = fullMap.length; i > top; i--) // Checks bottom right quarter same way just in case no return yet.
		{
			for (int j = fullMap[0].length; j > left; j--) {
				if (fullMap[i][j] != fullMap[fullMap.length - 1 - i][j]) {
					return true;
				} else if (fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j]) {
					return false;
				}
			}
		}

		log("it's frickin reflected both ways >:(");
		return true;
	}

	private void getHomeCastle()
	{
		for(Robot rob : getVisibleRobots())
		{
			if(rob.unit == SPECS.CASTLE)
			{
				castleLocs[0] = new int[] {rob.x, rob.y};
				robs[0].add(rob.id);
				globalTurn = rob.turn;
				if(me.unit == SPECS.PILGRIM)
				{
					HOME = new int[] { rob.signal % 64, (int) (rob.signal / 64) };
				}
			}
		}
	}

	private int[] findClosestKarbo() {
		int minDistance = fullMap.length * fullMap.length;
		int[] ans;
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
		int[] ans = new int[] { 0, 0 };
		for (int x = 0; x < fullMap[0].length; x++) {
			looping: for (int y = 0; y < fullMap.length; y++) {
				if (fullMap[y][x] == FUEL) {
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

	private void setXorKey()
	{
		int[] parts = new int[4];
		parts[0] = 5 + fullMap[9][30] + fullMap[18][8] + fullMap[9][0] + fullMap[23][28] + fullMap[15][31];
		parts[1] = 5 + fullMap[19][3] + fullMap[31][8] + fullMap[10][26] + fullMap[11][11] + fullMap[4][2];
		parts[2] = 5 + fullMap[6][9] + fullMap[4][20] + fullMap[13][3] + fullMap[18][29] + fullMap[19][12];
		parts[3] = 5 + fullMap[30][10] + fullMap[31][31] + fullMap[0][0] + fullMap[5][15] + fullMap[1][8];

		xorKey = parts[3] * 4096 + parts[2] * 256 + parts[1] * 16 + parts[0];
	}

	private void getEnemyCastleLocs()
	{
		for(int i = 0; i < 3; i++)
		{
			if(hRefl)
			{
				enemyCastleLocs[i][0] = fullMap.length - 1 - castleLocs[i][0];
				enemyCastleLocs[i][1] = castleLocs[i][1];
			}
			else
			{
				enemyCastleLocs[i][0] = castleLocs[i][0];
				enemyCastleLocs[i][1] = fullMap.length - 1 - castleLocs[i][1];
			}
		}
	}

	private void getTargetCastle()
	{
		if(numCastles == 1)
		{
			targetCastle = 0;
		}
		else if(numCastles == 2)
		{
			targetCastle = fullMap[17][22] == 0 ? 0 : 1;				
		}
		else if(numCastles == 3)
		{
			int minInd, maxInd;
			int min = 64;
			int max = -1;

			for(int i = 0; i < 3; i++)
			{
				if(enemyCastleLocs[i][hRefl ? 1 : 0] < min)
				{
					minInd = i;
					min = enemyCastleLocs[i][hRefl ? 1 : 0];
				}
				if(enemyCastleLocs[i][hRefl ? 1 : 0] > max)
				{
					maxInd = i;
					max = enemyCastleLocs[i][hRefl ? 1 : 0];
				}
			}

			enemyCastleLocs = new int[][] {enemyCastleLocs[minInd], enemyCastleLocs[3 - minInd - maxInd], enemyCastleLocs[maxInd]};

			targetCastle = fullMap[22][17] == 0 ? 2 : 0;
		}
		else
		{
			log("uh oh numCastles is " + numCastles);
		}
	}

	private Robot[] getEnemiesInRange() {
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enms = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) <= SPECS.UNITS[me.unit].ATTACK_RADIUS[1]
					&& (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) >= SPECS.UNITS[me.unit].ATTACK_RADIUS[0]) {
				enms.add(rob);
			}
		}

		return enms.toArray(new Robot[enms.size()]);
	}

	private int[] autoAttack() // Returns null if you don't have enough fuel!
	{
		if(fuel < SPECS.UNITS[me.unit].ATTACK_FUEL_COST)
		{
			return null;
		}

		Robot[] robs = getEnemiesInRange();

		if (robs.length == 0) {
			return null;
		}

		ArrayList<Robot> priorRobs = new ArrayList<Robot>(); // only robots of highest priority type
		boolean found = false;
		int i = 0;

		while (!found && i < 6) // make priorRobs
		{
			for (Robot rob : robs) {
				if (rob.unit == attackPriority[i]) {
					found = true;
					priorRobs.add(rob);
				}
			}
			i++;
		}

		if (priorRobs.size() == 1) {
			return new int[] {priorRobs.get(0).x - me.x, priorRobs.get(0).y - me.y};
		} else if (priorRobs.size() == 0) {
			log("why are there no enemies and yet autoAttack() has gotten all the way here");
			return null;
		}

		int lowestID = 4097;
		for (int j = 0; j < priorRobs.size(); j++) {
			if (priorRobs.get(j).id < lowestID) {
				lowestID = priorRobs.get(j).id;
			}
		}

		return new int[] {getRobot(lowestID).x - me.x, getRobot(lowestID).y - me.y};
	}

	// For preacherAttack()
	private Robot[] getPreacherKillableRobots() // Now returns only units with max health <= 20 in visibility range, but
	// can be edited
	{ // to also return damaged units or units 1 space outside visibility range
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> killable = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.unit == SPECS.PILGRIM || rob.unit == SPECS.PROPHET)) {
				killable.add(rob);
			}
		}

		return killable.toArray(new Robot[killable.size()]);
	}

	// For preacherAttack()
	private Robot[] getAllies() // Now returns only visible allies, but preachers can damage non-visible allies
	// :( plis update
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> allies = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team == me.team) {
				allies.add(rob);
			}
		}

		return allies.toArray(new Robot[allies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyRobots() // Does not return from outside of visibility range :(
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> enemies = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && (rob.unit == SPECS.CRUSADER || rob.unit == SPECS.PREACHER || rob.unit == SPECS.CASTLE)) {
				enemies.add(rob);
			}
		}

		return enemies.toArray(new Robot[enemies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyChurches() // Does not return from outside of visibility range :(
	{ // Do not combine with other preacherAttack() helper methods. Ask Zain for elaboration if wanted.
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> buildings = new ArrayList<Robot>();

		for (Robot rob : robs) {
			if (rob.team != me.team && rob.unit == SPECS.CHURCH)
			{
				buildings.add(rob);
			}
		}

		return buildings.toArray(new Robot[buildings.size()]);
	}

	// For preacherAttack()
	private boolean squareContainsRobot(Robot rob, int centerX, int centerY) // 3x3 square
	{
		if (rob.x + 1 >= centerX && rob.x - 1 <= centerX && rob.y + 1 >= centerY && rob.y - 1 <= centerY) {
			return true;
		}
		return false;
	}

	public AttackAction preacherAttack() // UNTESTED: Returns best attack for a preacher. No ally damage, then kill enemies, then damage
	{ // enemy combat units, then damage enemy buildings. Returns null if you don't have enough fuel.
		if(fuel < SPECS.UNITS[5].ATTACK_FUEL_COST)
		{
			return null;
		}

		Robot[] killable = getPreacherKillableRobots();
		Robot[] allies = getAllies();

		int[][] attackLocs = new int[9][9];
		ArrayList<int[]> bestLocs = new ArrayList<int[]>();
		bestLocs.add(new int[] {0, 0, 0}); // x, y, value

		for (int y = 0; y < 9; y++) // Killable units
		{
			for (int x = 0; x < 9; x++) {
				if (x == 3 && y > 2 && y < 6) {
					x += 3;
				}

				attackLocs[y][x] = 0;

				for (Robot ally : allies) {
					if (squareContainsRobot(ally, x, y)) {
						attackLocs[y][x] = -100;
					}
				}

				if (attackLocs[y][x] == 0) {
					for (Robot deathable : killable) {
						if (squareContainsRobot(deathable, x, y)) {
							attackLocs[y][x] += 1;
						}
					}
				}

				if (attackLocs[y][x] > bestLocs.get(0)[2]) {
					bestLocs.clear();
					bestLocs.add(new int[] { x, y, attackLocs[y][x] });
				} else if (attackLocs[y][x] == bestLocs.get(0)[2]) {
					bestLocs.add(new int[] { x, y, attackLocs[y][x] });
				}
			}
		}

		if (bestLocs.get(0)[2] == 0)
		{
			return null;
		}
		else if(bestLocs.size() == 1)
		{
			return attack(bestLocs.get(0)[0] - 4, bestLocs.get(0)[1] - 4);
		}

		Robot[] combat = getEnemyRobots();
		ArrayList<int[]> bestbestLocs = new ArrayList<int[]>();
		bestbestLocs.add(new int[] { 0, 0, -1 }); // x, y, new value

		for (int[] pos : bestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : combat) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > bestbestLocs.get(0)[2]) {
				bestbestLocs.clear();
				bestbestLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == bestbestLocs.get(0)[2]) {
				bestbestLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (bestbestLocs.size() == 1) {
			return attack(bestbestLocs.get(0)[0] - 4, bestbestLocs.get(0)[1] - 4);
		}

		Robot[] build = getEnemyChurches(); // write this to return all castles and churches
		ArrayList<int[]> goodLocs = new ArrayList<int[]>();
		goodLocs.add(new int[] { 0, 0, -1 }); // x, y, new value

		for (int[] pos : bestbestLocs) // Tiebreakers based on most damage to other combat units
		{
			for (Robot rob : build) {
				if (squareContainsRobot(rob, pos[0], pos[1])) {
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if (attackLocs[pos[1]][pos[0]] > goodLocs.get(0)[2]) {
				goodLocs.clear();
				goodLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			} else if (attackLocs[pos[1]][pos[0]] == goodLocs.get(0)[2]) {
				goodLocs.add(new int[] { pos[0], pos[1], attackLocs[pos[1]][pos[0]] });
			}
		}

		if (goodLocs.size() == 1) {
			return attack(goodLocs.get(0)[0] - 4, goodLocs.get(0)[1] - 4);
		}


		int lowestID = 4097;
		int[] finalBestLoc = new int[] {-1, -1};

		for (int[] loc : goodLocs) // Tiebreaks to attack single robot with lowest ID
		{
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int newX = me.x + loc[0] - 4 + dx;
					int newY = me.y + loc[1] - 4 + dy;
					if(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length)
					{
						continue;
					}
					int ID = robotMap[newY][newX];
					if (ID > 0 && ID < lowestID && getRobot(ID).team != me.team) {
						lowestID = ID;
						finalBestLoc = new int[] {loc[0], loc[1]};
					}
				}
			}
		}

		if(finalBestLoc[0] == -1)
		{
			log("aha!");
			return null;
		}

		return attack(finalBestLoc[0] - me.x, finalBestLoc[1] - me.y);
	}

	private void updateTargetCastle()
	{
		boolean castleKilled = true;
		int newX, newY;

		for(int dx = -2; dx <= 2; dx++)
		{
			for(int dy = -2; dy <= 2; dy++)
			{
				newX = enemyCastleLocs[targetCastle][0] + dx;
				newY = enemyCastleLocs[targetCastle][1] + dy;

				if(!(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length))
				{
					int ID = robotMap[newY][newX];
					if(ID == -1 || (ID > 0 && getRobot(ID).team != me.team))
					{
						castleKilled = false;
					}
				}
			}
		}

		if(castleKilled)
		{
			enemyCastleLocs[targetCastle] = new int[] {-1, -1};

			if(numCastles == 2)
			{
				targetCastle = 1 - targetCastle;
			}

			else if(enemyCastleLocs[1][0] == -1)
			{
				targetCastle = enemyCastleLocs[0][0] != -1 ? 0 : 2;
			}
			else
			{
				targetCastle = 1;
			}
		}
	}

	// bfs is reaaaally fast now
	private ArrayList<int[]> bfs(int goalX, int goalY) {
		locInPath = 0;

		boolean occupied = false;
		if (robotMap[goalY][goalX] > 0) {
			occupied = true;
		}
		int fuelCost = SPECS.UNITS[me.unit].FUEL_PER_MOVE;
		int maxRadius = (int) Math.sqrt(SPECS.UNITS[me.unit].SPEED);
		LinkedList<int[]> spots = new LinkedList<>();
		int[] spot = new int[] { me.x, me.y };
		int[] from = new int[fullMap.length * fullMap.length];
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
							&& (robotMap[y][x] <= 0/* || getRobot(robotMap[y][x]).unit < 2*/)) {
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
		int[][] robomap = robotMap;
		for (int[] adj : adjacentSpaces) {
			if (y + adj[1] > -1 && y + adj[1] < robomap.length && x + adj[0] > -1 && x + adj[0] < robomap.length)
			{
				if (robomap[y + adj[1]][x + adj[0]] > 0) {
					return false;
				}
			}
		}
		return true;
	}

	// mormons may be polygamists but no touchie touchie
	private ArrayList<int[]> bfsCooties(int goalX, int goalY) {
		locInPath = 0;

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
					if (dx * dx + dy * dy <= maxRadius * maxRadius 
							&& fullMap[y][x] > IMPASSABLE 
							&& robotMap[y][x] <= 0
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
				//log("exhausted all options");
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

	private int[] availAdjSq(int[] target)
	{
		int i;
		if(target[0] == 0)
		{
			i = target[1] * -2 + 2;
		}
		else if(target[0] == -1)
		{
			i = target[1] * -1 + 2;
		}
		else if(target[0] == 1)
		{
			i = target[1] + 6;
		}
		else // NOTE: THIS WILL NOT CATCH ALL INVALID TARGETS, ONLY INVALID TARGET[0]S
		{
			log("That is not a valid target for availAdjSq(). Returning null.");
			return null;
		}

		int newX = me.x + adjacentSpaces[i][0];
		int newY = me.y + adjacentSpaces[i][1];

		int delta = 1;
		int sign = 1;

		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0)
		{
			if(delta >= 8)
			{
				log("No adjacent movable spaces (from availAdjSq()).");
				return null;
			}

			i += delta * sign;
			i %= 8;

			newX = me.x + adjacentSpaces[i][0];
			newY = me.y + adjacentSpaces[i][1];

			delta += 1;
			sign *= -1;
		}

		return adjacentSpaces[i];
	}

	private int[] randomAdjSq()
	{
		int rand, newX, newY;
		rand = (int) (Math.random() * 8);
		int i = 0;
		do
		{
			rand += 1;
			rand %= 8;
			i++;
			newX = me.x + adjacentSpaces[rand][0];
			newY = me.y + adjacentSpaces[rand][1];

			if(i >= 8)
			{
				//				log("No adjacent movable spaces (from randomAdjSq()).");
				return null;
			}
		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0);

		return adjacentSpaces[rand];
	}

	private int[] randomOddAdjSq()
	{
		int rand, newX, newY;
		int pos = 1 - (me.x + me.y) % 2;

		rand = ((int) (Math.random() * 4)) * 2 + 1 + pos;
		int i = 0;
		do
		{
			i++;
			if(i > 4)
			{
				return null;
			}

			rand += 2;
			rand %= 8;
			newX = me.x + adjacentSpaces[rand][0];
			newY = me.y + adjacentSpaces[rand][1];

		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] == -1 || robotMap[newY][newX] > 0);

		return adjacentSpaces[rand];
	}

	private void getCastleDir()
	{
		if(castleLocs[0][0] - me.x == 0)
		{
			castleDir = castleLocs[0][1] - me.y * -2 + 2;
		}
		else if(castleLocs[0][0] - me.x == -1)
		{
			castleDir = castleLocs[0][1] - me.y * -1 + 2;
		}
		else if(castleLocs[0][0] - me.x == 1)
		{
			castleDir = castleLocs[0][1] - me.y + 6;
		}
	}

	private int[] exploreLattice()
	{
		int[] fpoo;
		int newX, newY;

		if(castleDir % 2 == 0)
		{
			int chooseDir = (int) (Math.random() * 2);
			fpoo = (chooseDir == 0) ? (adjacentSpaces[(castleDir + 4) % 8]) : adjacentSpaces[sideDir];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] == 0 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}

			fpoo = (chooseDir != 0) ? (adjacentSpaces[(castleDir + 4) % 8]) : adjacentSpaces[sideDir];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] == 0 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}
		}
		else
		{
			int chooseDir = ((int) (Math.random() * 2)) * 2 - 1;
			fpoo = adjacentSpaces[(castleDir + 4 + chooseDir) % 8];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] == 0 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}

			fpoo = adjacentSpaces[(castleDir + 4 - chooseDir) % 8];
			fpoo = new int[] {fpoo[0] * 2, fpoo[1] * 2};

			newX = me.x + fpoo[0];
			newY = me.y + fpoo[1];
			if(newX >= 0 && newX < fullMap.length && newY >= 0 && newY < fullMap.length && fullMap[newY][newX] == 0 && robotMap[newY][newX] <= 0)
			{
				return fpoo;
			}
		}

		return null;
	}

	private boolean isNextToHome(int newX, int newY)
	{
		if(Math.abs(newX - castleLocs[0][0]) <= 1 && Math.abs(newY - castleLocs[0][1]) <= 1)
		{
			return true;
		}
		return false;
	}

	private ArrayList<int[]> goToLattice()
	{
		int newX, newY, rRange;
		for(int range = 1; range < 64; range++)
		{
			rRange = (int) (Math.floor(Math.sqrt(range)));
			for(int dx = -rRange; dx <= rRange; dx++)
			{
				for(int dy = -rRange; dy <= rRange; dy++)
				{
					if(dx * dx + dy * dy == range)
					{
						newX = me.x + dx;
						newY = me.y + dy;
						if(isOnMap(newX, newY) && fullMap[newY][newX] == 0 && robotMap[newY][newX] <= 0 && (newX + newY) % 2 == 0 && !isNextToHome(newX, newY))
						{
							return bfs(newX, newY);
						}
					}
				}
			}
		}
		return null;
	}

	/*private int[] latticify()
	{
		int newX, newY;
		int dir = ((int) (Math.random() * 4)) * 2;
		int i = 0;

		do
		{
			i++;
			if(i > 4)
			{
				return null;
			}
			newX = me.x + adjacentSpaces[dir][0];
			newY = me.y + adjacentSpaces[dir][1];

			dir += 2;
			dir %= 8;

			if(me.unit == 5)
			{
			}
		}
		while(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length || fullMap[newY][newX] != 0 || robotMap[newY][newX] > 0);

		return adjacentSpaces[(dir + 6) % 8];
	}*/

	private boolean moveAway()
	{
		for(int dx = -2; dx <= 2; dx++)
		{
			for(int dy = -2; dy <= 2; dy++)
			{
				if((Math.abs(dx) == 2 && Math.abs(dy) == 2) || (dx == 0 && dy == 0))
				{
					continue;
				}

				int newX = me.x + dx;
				int newY = me.y + dy;
				if(!(newX < 0 || newX >= fullMap.length || newY < 0 || newY >= fullMap.length))
				{
					int ID = robotMap[newY][newX];
					if(ID > 0 && getRobot(ID).team == me.team && (getRobot(ID).unit == 4 || getRobot(ID).unit == 0  || getRobot(ID).unit == 1))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isOnMap(int x, int y)
	{
		return (x >= 0 && x < fullMap.length && y >= 0 && y < fullMap.length);
	}

	private void getNewUnit(int talk)
	{
		for(Robot rob : getVisibleRobots())
		{
			if(rob.unit == talk)
			{
				boolean n = true;
				for(Integer oldRobID : robs[talk])
				{
					if(rob.id == oldRobID)
					{
						n = false;
						break;
					}
				}
				if(n)
				{
					robs[talk].add(rob.id);
					break;
				}
			}
		}
	}

	private void getCastleLocs()
	{
		if(globalTurn == 849)
		{
			int talk = getCastObj(0).signal ^ xorKey;

			if(talk >= 4096)
			{
				numCastles = 1;
				getEnemyCastleLocs();
				getTargetCastle();
			}
			else
			{
				numCastles = 2;
				castleLocs[1][0] = talk % 64;
				castleLocs[1][1] = (int) Math.floor(talk / 64);
			}
		}
		else if(globalTurn == 850 && numCastles == 2)
		{
			int talk = getCastObj(0).signal ^ xorKey;
			if(talk < 4096)
			{
				numCastles = 3;
				castleLocs[2][0] = talk % 64;
				castleLocs[2][1] = (int) Math.floor(talk / 64);
			}
			getEnemyCastleLocs();
			getTargetCastle();
		}
	}

	private Robot getCastObj(int num)
	{
		Robot[] visb = getVisibleRobots();

		for(Robot cast : visb)
		{
			if(cast.id == robs[0].get(num))
			{
				return cast;
			}
		}
		return null;
	}

	private void updateRobs() // for castles
	{
		Robot[] visb = getVisibleRobots();
		ArrayList<Integer> unchecked = new ArrayList<Integer>();
		for(int i = 0; i < 6; i++)
		{
			unchecked.addAll(robs[i]);
		}

		for(Robot r : visb)
		{
			if(r.team == me.team)
			{
				if(unchecked.contains(r.id))
				{
					unchecked.removeAll(Arrays.asList(r.id));
				}
				else if(r.turn == 1)
				{
					robs[r.castle_talk].add(r.id);
				}
				else if(r.turn > 1)
				{
					log("Turn: " + me.turn + ". Robot " + r.id + " is not in robs after their first turn.");
				}
			}
		}

		for(Integer dead : unchecked)
		{
			for(int i = 0; i < 6; i++)
			{
				if(robs[i].contains(dead))
				{
					robs[i].removeAll(Arrays.asList(dead));
					if(i == 0)
					{
						ourDeadCastles += 1;
					}
					break;
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

		for (int x = 0; x < fullMap.length; x++)
		{
			for (int y = 0; y < fullMap.length; y++)
			{
				if (fullMap[y][x] == KARBONITE)
				{
					allKarbos[karboIndex][0] = x;
					allKarbos[karboIndex++][1] = y;
				}
				else if (fullMap[y][x] == FUEL)
				{
					allFuels[fuelIndex][0] = x;
					allFuels[fuelIndex++][1] = y;
				}
			}
		}
	}

	private void fillAllMines()
	{
		allMines = new int[numKarbos + numFuels][];
		int i = 0;
		for (int[] mine : allKarbos) {
			allMines[i] = mine;
			i++;
		}
		for (int[] mine : allFuels) {
			allMines[i] = mine;
			i++;
		}
	}

	// call this on turn 1 after fillAllMines
	private void getMineScores() {
		allMineScores = new int[allMines.length];
		int index = 0;
		for (int[] check : allMines) {
			int count = 0;
			for (int[] mine : allMines) {
				if (tilesInRange(check, mine, mineClusterRadiusSqrd)) {
					count++;
				}
			}
			allMineScores[index++] = count;
		}
	}

	private void identifyClusters() {
		ArrayList<int[]> scannedMines = new ArrayList<>();
		mineClusters = new ArrayList<>();
		while (scannedMines.size() != allMines.length) {
			int bestIndex = -1;
			for (int i = 0; i < allMineScores.length; i++) {
				if (scannedMines.contains(allMines[i])) {
					continue;
				}
				if (bestIndex == -1 || allMineScores[i] > allMineScores[bestIndex]) {
					bestIndex = i;
				}
			}
			int[] head = allMines[bestIndex];
			ArrayList<int[]> currentCluster = new ArrayList<>();
			currentCluster.add(head);
			scannedMines.add(head);
			for (int[] mine : allMines) {
				if (scannedMines.contains(mine)) {
					continue;
				}
				if (tilesInRange(head, mine, mineClusterRadiusSqrd)) {
					currentCluster.add(mine);
					scannedMines.add(mine);
				}
			}
			mineClusters.add(currentCluster);
		}
	}

	private void findClusterCenters() {
		mineClusterCenters = new ArrayList<>();
		for (ArrayList<int[]> cluster : mineClusters) {
			int clusterSize = cluster.size();
			double cumulativeX = 0.0;
			double cumulativeY = 0.0;
			for (int[] mine : cluster) {
				cumulativeX += mine[0];
				cumulativeY += mine[1];
			}
			int proposedX = (int) Math.round(cumulativeX / clusterSize);
			int proposedY = (int) Math.round(cumulativeY / clusterSize);
			int[] closestMatch = new int[] { 0, 0 };
			int closestDistance = proposedX * proposedX + proposedY * proposedY;
			for (int checkX = 0; checkX < fullMap[0].length; checkX++) {
				for (int checkY = 0; checkY < fullMap.length; checkY++) {
					if (fullMap[checkY][checkX] != PASSABLE) {
						continue;
					}
					int distance = (checkX - proposedX) * (checkX - proposedX)
							+ (checkY - proposedY) * (checkY - proposedY);
					if (distance < closestDistance) {
						closestDistance = distance;
						closestMatch[0] = checkX;
						closestMatch[1] = checkY;
					}
				}
			}
			mineClusterCenters.add(closestMatch);
		}
		isMineColonized = new boolean[mineClusterCenters.size()];
	}

	private boolean tilesInRange(int[] tile1, int[] tile2, int rangeSquared) {
		return ((tile1[0] - tile2[0]) * (tile1[0] - tile2[0])
				+ (tile1[1] - tile2[1]) * (tile1[1] - tile2[1]) <= rangeSquared);
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
}