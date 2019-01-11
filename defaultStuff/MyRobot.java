package bc19;

import bc19.*;
import java.util.ArrayList;

public class MyRobot extends BCAbstractRobot {
	public int turn;
	private final int IMPASSABLE = -1;
	private final int PASSABLE = 0;
	private final int KARBONITE = 1;
	private final int FUEL = 2;
	private final int[] attackPriority = new int[] {4, 5, 3, 0, 1, 2}; // 0: Castle, 1: Church, 2: Pilgrim, 3: Crusader, 4: Prophet,
	private boolean hRefl; // true iff reflected horizontally				 5: Preacher. Feel free to mess with order in your robots.
	private int[][] fullMap; // 0: normal, 1: impassible, 2: karbonite, 3: fuel

	public Action turn() {
		turn++;

		if(turn == 1)
		{
			getFMap();
			hRefl = getReflDir();
			
	/*		if(hRefl) // Testing
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

		if (me.unit == SPECS.CASTLE) { return castle(); }
		else if (me.unit == SPECS.CHURCH) { return church(); }
		else if (me.unit == SPECS.PILGRIM) { return pilgrim(); }
		else if (me.unit == SPECS.CRUSADER) { return crusader(); }
		else if (me.unit == SPECS.PROPHET) { return prophet(); }
		else if (me.unit == SPECS.PREACHER) { return preacher(); }
		return null;
	}

	public Action castle()
	{
	//	log(Arrays.deepToString(fullMap));
		return null;
	}


	public Action church()
	{
		return null;
	}


	public Action pilgrim()
	{
		return null;
	}


	public Action crusader()
	{
		return null;
	}


	public Action prophet()
	{
		return null;
	}


	public Action preacher()
	{
		return null;
	}


	public void getFMap() // makes fullMap
	{
		boolean[][] m = getPassableMap();
		boolean[][] k = getKarboniteMap();
		boolean[][] f = getFuelMap();
		
		fullMap = new int[m.length][m[0].length];
		
		int h = m.length;
		int w = m[0].length;

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

	public boolean getReflDir() // set hRefl
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
	
	public AttackAction autoAttack() // NOT TESTED: Attacks unit in attack range of type earliest in attackPriority, of lowest ID
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