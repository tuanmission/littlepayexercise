
import java.util.Date;

import org.json.*;
public class tap {
	public Date tapdate;
	public String busid;
	public String companyid;
	public String stopid;
	public TapType taptype;
	public tap(Date dt, String busid, String companyId, String stopid, TapType tt) {
		this.tapdate=dt;
		this.busid= busid;
		this.companyid= companyId;
		this.taptype=tt;
		this.stopid = stopid;
	}
	

}
