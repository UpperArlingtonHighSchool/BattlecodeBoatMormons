package zainsExperiments;

import src.library.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
    	turn++;

    	if (me.unit == SPECS.CASTLE) { castle(); }
    	else if (me.unit == SPECS.CHURCH) { church(); }
    	else if (me.unit == SPECS.PILGRIM) { pilgrim(); }
    	else if (me.unit == SPECS.CRUSADER) { crusader(); }
    	else if (me.unit == SPECS.PROPHET) { prophet(); }
    	else if (me.unit == SPECS.PREACHER) { preacher(); }
    	return null;
	}
    
    public Action castle()
    {
    	
    }
    
    
    public Action church()
    {
    	
    }
    
    
    public Action pilgrim()
    {
    	
    }
    
    
    public Action crusader()
    {
    	
    }
    
    
    public Action prophet()
    {
    	
    }
    
    
    public Action preacher()
    {
    	
    }
}