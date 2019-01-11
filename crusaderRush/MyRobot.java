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
	private int numCastles = 1;
	

	public Action turn() {
		turn++;

		if(turn == 1)
		{
			getFMap();
			hRefl = getReflDir();
			
			if(me.unit == SPECS.CASTLE) // set numCastles
			{
				for(Robot rob : getVisibleRobots())
				{
					if(rob.team == me.team)
					{
						numCastles += 1;
					}
				}
			}
		}

		if (me.unit == SPECS.CASTLE) { return castle(); }
		else if (me.unit == SPECS.CHURCH) { return church(); }
		else if (me.unit == SPECS.PILGRIM) { return pilgrim(); }
		else if (me.unit == SPECS.CRUSADER) { return crusader(); }
		else if (me.unit == SPECS.PROPHET) { return prophet(); }
		else if (me.unit == SPECS.PREACHER) { return preacher(); }
		return null;
	}

	private Action castle()
	{
		return null;
	}


	private Action church()
	{
		return null;
	}


	private Action pilgrim()
	{
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
			log("you're trying to attack with a non-combat robot and autoAttack() is prolly gonna return an error");
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
}