package uc.game.statistics.config;

public enum OperationType {
	COUNT {
		@Override
		public String getStatisticsKpiSql(String fileName) {
			return "count("+fileName+")";
		}
	},
	SUM {
		@Override
		public String getStatisticsKpiSql(String fileName) {
			return "sum("+fileName+")";
		}
	},
	MIN {
		@Override
		public String getStatisticsKpiSql(String fileName) {
			return "min("+fileName+")";
		}
	},
	MAX {
		@Override
		public String getStatisticsKpiSql(String fileName) {
			return "max("+fileName+")";
		}
	},
	UNIQUE {
		@Override
		public String getStatisticsKpiSql(String fileName) {
			return "count(distinct "+fileName+") ";
		}
	};
	public abstract String getStatisticsKpiSql(String fileName);
}
