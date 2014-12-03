package server;

public enum Operation {
	WITHDRAW, DEPOSIT;

	public static Operation getEnumFromString(String string) {
		if (string != null) {
			try {
				return Enum.valueOf(Operation.class, string.trim());
			} catch (IllegalArgumentException ex) {
			}
		}
		return null;
	}
}
