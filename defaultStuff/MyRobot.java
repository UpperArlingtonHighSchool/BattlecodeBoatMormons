package bc19;

import bc19.*;
import java.util.ArrayList;

public class MyRobot extends BCAbstractRobot {
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private final int[] attackPriority = new int[] {4, 5, 3, 0, 1, 2}; // 0: Castle, 1: Church, 2: Pilgrim, 3: Crusader, 4: Prophet,
	private boolean hRefl; // true iff reflected horizontally				 5: Preacher. Feel free to mess with order in your robots.
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel
	private int numCastles;
	private int[] castleIDs = new int[3]; // small so we don't worry about if there's only 1 or 2 castles
	private int[][] plainCastleLocs = new int[3][2]; // {{x, y}, {x, y}, {x, y}}
	private int[] encodedCastleLocs = new int[3];
	private int encodedLocError; // Only for use by castles in first few turns
	private final int[][] adjacentSpaces = new int[][] { //Matrix of adjacent spaces, relative to the Robot
		new int[] {0,1},
		new int[] {-1,1},
		new int[] {-1,0},
		new int[] {-1,-1},
		new int[] {0,-1},
		new int[] {1,-1},
		new int[] {1,0},
		new int[] {1,1}
	}; 

	public Action turn() {
		if(me.turn == 1)
		{
			getFMap();
			hRefl = getReflDir();

			/*		if(hRefl) // Testing hRefl and fullMap
			{
				log("hor");
			}
			else
			{
				log("vert");
			}

			String boop;
			for(int[] r : fullMap)
			{
				boop = "";
				for(int c : r)
				{
					boop += c + " ";
				}
				log(boop);
			}*/
		}

		switch (me.unit) {
		case 0: return castle();
		case 1: return church();
		case 2: return pilgrim();
		case 3: return crusader();
		case 4: return prophet();
		case 5: return preacher();
		}
		return null;
	}

	private Action castle()
	{
		if(me.turn == 1)
		{
			numCastles = 1;
			castleIDs[0] = me.id;

			for(Robot rob : getVisibleRobots())
			{
				if(rob.team == me.team && rob.id != me.id)
				{
					castleIDs[numCastles] = rob.id;
					numCastles += 1;
				}
			}

			if(numCastles > 1)
			{
				sendOwnLoc();
			}
		}

		else if(me.turn == 2)
		{
			if(numCastles > 1)
			{
				sendOwnLoc();
			}

			for (int i = 1; i < numCastles; i++)
			{
				encodedCastleLocs[i] = getRobot(castleIDs[i]).castle_talk;
				decodeCastleLoc(i);
			}
			
			sendCastleLocs(1);
			return buildUnit(SPECS.PILGRIM, 0, 1);
		}

		else if(me.turn == 3)
		{
			castleTalk(encodedLocError); // Only 2 bits so feel free to add more info and also quite unimportant overall
		}

		else if(me.turn == 4)
		{
			castleTalk(encodedLocError); // Only 2 bits so feel free to add more info and also quite unimportant overall

			for (int i = 1; i < numCastles; i++)
			{
				fixLocError(getRobot(castleIDs[i]).castle_talk, i);
			}
			
			/*String str  = "{"; // Testing that castles know where all castles are 
			for(int i = 0; i < numCastles; i++)
			{
				str += "{";
				for(int j = 0; j < 2; j++)
				{
					str += plainCastleLocs[i][j] + ", ";
				}
				str = str.substring(0, str.length() - 2) + "}, ";
			}
			str = str.substring(0, str.length() - 2) + "}";
			log(str);*/
		}
	}


	private Action church()
	{
		return null;
	}


	private Action pilgrim()
	{
		if(me.turn == 1)
		{
			getAllCastleLocs();

			/*String str  = "{"; // Testing that pilgrims know where all castles are 
			for(int i = 0; i < numCastles; i++)
			{
				str += "{";
				for(int j = 0; j < 2; j++)
				{
					str += plainCastleLocs[i][j] + ", ";
				}
				str = str.substring(0, str.length() - 2) + "}, ";
			}
			str = str.substring(0, str.length() - 2) + "}";
			log(str);*/
		}
		return null;
	}


	private Action crusader()
	{
		return null;
	}


	private Action prophet()
	{
		return null;
	}


	private Action preacher()
	{
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

		for(int i = 0; i < h; i++)
		{
			for(int j = 0; j < w; j++)
			{
				if(!m[i][j])
				{
					fullMap[i][j] = IMPASSABLE;
				}
				else if(k[i][j])
				{
					fullMap[i][j] = KARBONITE;
				}
				else if(f[i][j])
				{
					fullMap[i][j] = FUEL;
				}
				else
				{
					fullMap[i][j] = PASSABLE;
				}
			}
		}
	}

	private boolean getReflDir() // set hRefl
	{
		int top = (fullMap.length + 1) / 2;
		int left = (fullMap[0].length + 1) / 2;

		for(int i = 0; i < top; i++)	// Goes through top left quarter of map and tests one cell at a time
		{								// for whether it's reflected horizontally then vertically.
			for(int j = 0; j < left; j++) // If a discrepancy is found, method returns.
			{
				if(fullMap[i][j] != fullMap[fullMap.length - 1 - i][j])
				{
					return true;
				}
				else if(fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j])
				{
					return false;
				}
			}
		}

		for(int i = fullMap.length; i > top; i--) // Checks bottom right quarter same way just in case no return yet.
		{
			for(int j = fullMap[0].length; j > left; j--)
			{
				if(fullMap[i][j] != fullMap[fullMap.length - 1 - i][j])
				{
					return true;
				}
				else if(fullMap[i][j] != fullMap[i][fullMap[0].length - 1 - j])
				{
					return false;
				}
			}
		}

		return true; // If it gets here, it's reflected both ways.
	}

	private void sendOwnLoc() // Call first and second turn for castles to send their location to other castles
	{
		int[] plain; // 0 is location on your half of map; 1 is how far across
		int[] encoded = new int[2]; // ditto above
		int temp;

		if(hRefl)
		{
			plain = new int[] {me.y, me.x};
		}
		else
		{
			plain = new int[] {me.x, me.y};
		}

		encodedLocError = 0;
		encodedLocError += plain[0] % 2;

		encoded[0] = (int) Math.floor(plain[0] / 2);
		if(plain[1] >= fullMap.length / 2) // Same thing for opposite sides of the map
		{
			temp = (plain[1] - (int) Math.floor(fullMap.length / 2) - 8);
			encodedLocError += 2 * (temp % 2);
			encoded[1] = (int) Math.floor(temp / 2);
		}
		else
		{
			temp = (plain[1] - 3);
			encodedLocError += 2 * (temp % 2);
			encoded[1] = (int) Math.floor(temp / 2);
		}
		
		if(encoded[1] >= 8)
		{
			log("encoded location across value was too big (it was " + encoded[1] + "), it has been set to 7.");
			encoded[1] = 7;
		}

		if(encoded[0] >= 32 || encoded[0] < 0)
		{
			log("oh no encoded[0] is " + encoded[0]);
		}
		if(encoded[1] >= 8 || encoded[1] < 0)
		{
			log("oh no encoded[1] is " + encoded[1]);
		}

		plainCastleLocs[0] = new int[] {me.x, me.y};
		encodedCastleLocs[0] = encoded[0] * 8 + encoded[1];
		castleTalk(encodedCastleLocs[0]);
	}

	private void decodeCastleLoc(int i) // Tell it which index of encodedCastleLocs to decode, it'll put result in corresponding
	{									// index of plainCastleLocs.
		int[] plain = new int[2];

		plain[0] = (int) Math.floor(encodedCastleLocs[i] / 8) * 2;

		if((hRefl && me.x < fullMap.length / 2) || (!hRefl && me.y < fullMap.length / 2))
		{
			plain[1] = ((int) Math.floor(encodedCastleLocs[i] % 8) * 2) + 3;
		}
		else
		{
			plain[1] = ((int) Math.floor(encodedCastleLocs[i] % 8) * 2) + (int) Math.floor(fullMap.length / 2) + 8;
		}


		if(hRefl)
		{
			plainCastleLocs[i][0] = plain[1];
			plainCastleLocs[i][1] = plain[0];
		}
		else
		{
			plainCastleLocs[i] = plain;
		}
	}

	private void fixLocError(int adjustment, int i) // Namely, the small error due to compression in stored location of other castles
	{
		if(hRefl)
		{
			plainCastleLocs[i][1] += adjustment % 2;
			plainCastleLocs[i][0] += (int) Math.floor(adjustment / 2);
		}
		else
		{
			plainCastleLocs[i][0] += adjustment % 2;
			plainCastleLocs[i][1] += (int) Math.floor(adjustment / 2);
		}
	}

	private void sendCastleLocs(int r2) // Whenever you make a pilgrim, call this. Will give
	{		// how far away pilgrim has to be in each direction to be closer to other castle.

		if(numCastles == 2)
		{
			signal(encodedCastleLocs[1] * 257, r2);
		}
		else if(numCastles == 3)
		{
			signal(encodedCastleLocs[1] * 256 + encodedCastleLocs[0], r2);
		}
		else if(numCastles != 1)
		{
			log("oh no numCastles is " + numCastles);
		}
	}

	private void getAllCastleLocs() // Call on first turn of unit to get locations of and number of castles
	{
		for(Robot rob : getVisibleRobots())
		{
			if(rob.unit == SPECS.CASTLE)
			{
				plainCastleLocs[0] = new int[] {rob.x, rob.y};

				if(isRadioing(rob))
				{
					encodedCastleLocs[1] = (int) Math.floor(rob.signal / 256);
					encodedCastleLocs[2] = (int) Math.floor(rob.signal % 256);

					if(encodedCastleLocs[1] == encodedCastleLocs[2])
					{
						numCastles = 2;
						decodeCastleLoc(1);
					}
					else
					{
						numCastles = 3;
						decodeCastleLoc(1);
						decodeCastleLoc(2);
					}
				}
				else
				{
					numCastles = 1;
				}
			}
		}
	}

	private AttackAction autoAttack() // NOT TESTED: Attacks unit in attack range of type earliest in attackPriority, of lowest ID
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> priorRobs = new ArrayList<Robot>(); // only robots of highest priority type
		int minRange, range;
		if(me.id == 3 || me.id == 5)
		{
			minRange = 1;
			range = 16;
		}
		else if(me.id == 4)
		{
			minRange = 16;
			range = 64;
		}
		else
		{
			log("you're trying to attack with a non-combat robot and autoAttack() is gonna return an error");
			minRange = 0;
			range = 0;
		}

		boolean found = false;
		int i = 0;
		while(!found) // make priorRobs
		{
			for(Robot rob : robs)
			{
				if(rob.team != me.team && rob.unit == attackPriority[i] && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * 
						(rob.y - me.y) <= range && (rob.x - me.x) * (rob.x - me.x) + (rob.y - me.y) * (rob.y - me.y) >= minRange)
				{
					found = true;
					priorRobs.add(rob);
				}
			}
			i++;
		}

		int lowestID = 4097;
		for(int j = 0; j < priorRobs.size(); j++)
		{
			if(priorRobs.get(j).id < lowestID)
			{
				lowestID = priorRobs.get(j).id;
			}
		}

		return attack(getRobot(lowestID).x - me.x, getRobot(lowestID).y - me.y);
	}

	// For preacherAttack()
	private Robot[] getPreacherKillableRobots() // Now returns only units with max health <= 20 in visibility range, but can be edited
	{											//  to also return damaged units or units 1 space outside visibility range
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> killable = new ArrayList<Robot>();

		for(Robot rob : robs)
		{
			if(rob.team != me.team && (rob.unit == SPECS.PILGRIM || rob.unit == SPECS.PROPHET))
			{
				killable.add(rob);
			}
		}

		return killable.toArray(new Robot[killable.size()]);
	}

	// For preacherAttack()
	private Robot[] getAllies() // Now returns only visible allies, but preachers can damage non-visible allies :(	plis update
	{
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> allies = new ArrayList<Robot>();

		for(Robot rob : robs)
		{
			if(rob.team == me.team)
			{
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

		for(Robot rob : robs)
		{
			if(rob.team != me.team && (rob.unit == SPECS.CRUSADER || rob.unit == SPECS.PREACHER))
			{
				enemies.add(rob);
			}
		}

		return enemies.toArray(new Robot[enemies.size()]);
	}

	// For preacherAttack()
	private Robot[] getEnemyBuildings() // Does not return from outside of visibility range :(
	{									// Do not combine with other preacherAttack() helper methods. Ask Zain for elaboration if wanted.
		Robot[] robs = getVisibleRobots();
		ArrayList<Robot> buildings = new ArrayList<Robot>();

		for(Robot rob : robs)
		{
			if(rob.team != me.team && (rob.unit == SPECS.CASTLE || rob.unit == SPECS.CHURCH))
			{
				buildings.add(rob);
			}
		}

		return buildings.toArray(new Robot[buildings.size()]);
	}

	// For preacherAttack()
	private boolean squareContainsRobot(Robot rob, int centerX, int centerY) // 3x3 square
	{
		if(rob.x + 1 >= centerX && rob.x - 1 <= centerX && rob.y + 1 >= centerY && rob.y - 1 <= centerY)
		{
			return true;
		}
		return false;
	}

	public AttackAction preacherAttack() // UNTESTED: Returns best attack for a preacher. No ally damage, then kill enemies, then damage
	{									// enemy combat units, then damage enemy buildings
		Robot[] killable = getPreacherKillableRobots();
		Robot[] allies = getAllies();

		int[][] attackLocs = new int[9][9];
		ArrayList<Integer[]> bestLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] {0, 0, -1});						// x, y, value

		for(int y = 0; y < 9; y++) // Killable units
		{
			for(int x = 0; x < 9; x++)
			{
				if(x == 3 && y > 2 && y < 6)
				{
					x += 3;
				}

				attackLocs[y][x] = 0;

				for(Robot ally : allies)
				{
					if(squareContainsRobot(ally, x, y))
					{
						attackLocs[y][x] = -1;
					}
				}

				if(attackLocs[y][x] == 0)
				{
					for(Robot deathable: killable)
					{
						if(squareContainsRobot(deathable, x, y))
						{
							attackLocs[y][x] += 1;
						}
					}
				}

				if(attackLocs[y][x] > bestLocs.get(0)[2])
				{
					bestLocs.clear();
					bestLocs.add(new Integer[] {x, y, attackLocs[y][x]});
				}
				else if(attackLocs[y][x] == bestLocs.get(0)[2])
				{
					bestLocs.add(new Integer[] {x, y, attackLocs[y][x]});
				}
			}
		}
		if(bestLocs.size() == 1)
		{
			return attack(bestLocs.get(0)[0] - 4, bestLocs.get(0)[1] - 4);
		}

		Robot[] combat = getEnemyRobots(); // write this to return all crusaders and preachers
		ArrayList<Integer[]> bestbestLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] {0, 0, -1});						// x, y, new value

		for(Integer[] pos : bestLocs) // Tiebreakers based on most damage to other combat units
		{
			for(Robot rob : combat)
			{
				if(squareContainsRobot(rob, pos[0], pos[1]))
				{
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if(attackLocs[pos[1]][pos[0]] > bestbestLocs.get(0)[2])
			{
				bestbestLocs.clear();
				bestbestLocs.add(new Integer[] {pos[0], pos[1], attackLocs[pos[1]][pos[0]]});
			}
			else if(attackLocs[pos[1]][pos[0]] == bestbestLocs.get(0)[2])
			{
				bestbestLocs.add(new Integer[] {pos[0], pos[1], attackLocs[pos[1]][pos[0]]});
			}
		}

		if(bestbestLocs.size() == 1)
		{
			return attack(bestbestLocs.get(0)[0] - 4, bestbestLocs.get(0)[1] - 4);
		}

		Robot[] build = getEnemyBuildings(); // write this to return all castles and churches
		ArrayList<Integer[]> goodLocs = new ArrayList<Integer[]>();
		bestLocs.add(new Integer[] {0, 0, -1});						// x, y, new value

		for(Integer[] pos : bestbestLocs) // Tiebreakers based on most damage to other combat units
		{
			for(Robot rob : build)
			{
				if(squareContainsRobot(rob, pos[0], pos[1]))
				{
					attackLocs[pos[1]][pos[0]] += 1;
				}
			}

			if(attackLocs[pos[1]][pos[0]] > goodLocs.get(0)[2])
			{
				goodLocs.clear();
				goodLocs.add(new Integer[] {pos[0], pos[1], attackLocs[pos[1]][pos[0]]});
			}
			else if(attackLocs[pos[1]][pos[0]] == goodLocs.get(0)[2])
			{
				goodLocs.add(new Integer[] {pos[0], pos[1], attackLocs[pos[1]][pos[0]]});
			}
		}

		if(goodLocs.size() == 1)
		{
			return attack(goodLocs.get(0)[0] - 4, goodLocs.get(0)[1] - 4);
		}

		int lowestID = 4097;
		int[] finalBestLoc;
		int[][] robMap = getVisibleRobotMap();

		for(Integer[] loc : goodLocs) // Tiebreaks to attack single robot with lowest ID 
		{
			for(int dx = -1; dx <= 1; dx++)
			{
				for(int dy = -1; dy <= 1; dy ++)
				{
					int ID = robMap[loc[1] + dy][loc[0] + dx]; 

					if(ID > 0 && ID < lowestID)
					{
						lowestID = ID;
						finalBestLoc = new int[] {loc[0] + dx, loc[1] + dy};
					}
				}
			}
		}

		return attack(finalBestLoc[0] - 4, finalBestLoc[1] - 4);
	}
}