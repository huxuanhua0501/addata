package net.busonline.business.syn.pojo;

public enum CodeEnum {
	BEIJING("110000", "1"),
	SHANGHAI("310000", "4"), 
	GUANGZHOU("440100", "10"),
	SHENZHEN("440300","11"),
	CHENGDU("510100","24"),
	LBS("110000","101"),
	YUXIAZAI("110000","100");
	private CodeEnum(String code, String city) {
		this.code = code;
		this.city = city;
	}

	private String code;

	private String city;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public static void main(String[] args) {

		System.out.println(CodeEnum.BEIJING.getCode());
	}
}
