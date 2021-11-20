
import java.util.Date;

import org.json.*;
public class tap {
	private Date tapdate;
	private String busid;
	private String companyid;
	private String stopid;
	private TapType taptype;
	public tap(Date dt, String busid, String companyId, String stopid, TapType tt) {
		this.tapdate=dt;
		this.busid= busid;
		this.companyid= companyId;
		this.taptype=tt;
		this.stopid = stopid;
	}
	
	public Date getTapDate() {
		return this.tapdate;
		
	}
	
	public String getBusId() {
		return this.busid;
	}
	
	public String getCompanyId() {
		return this.companyid;
	}
	
	public String getStopId() {
		return this.stopid;
	}

	

	public TapType getTapType() {
		return this.taptype;
		
	}
	

}
