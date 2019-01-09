package bc19.bots.jaySandbox;

import bc19.*;

public class MyRobot extends BCAbstractRobot {
	public int turn;

	public Action turn() {
    	turn++;

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
}