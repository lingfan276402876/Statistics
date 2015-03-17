package uc.game.statistics.config;

public enum DataType {
	string {
		@Override
		public String getCreateTableSql(String fieldName) {
			return fieldName + " varchar(20) DEFAULT NULL";
		}
	},
	number {
		@Override
		public String getCreateTableSql(String fieldName) {
			return fieldName + " double DEFAULT NULL";
		}

		@Override
		public String changeInsertValue(String value) {
			return value;
		}
		
	};

	public abstract String getCreateTableSql(String fieldName);
	
	public String changeInsertValue(String value){
		return "'"+value+"'";
	}
}
