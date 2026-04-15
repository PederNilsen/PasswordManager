public enum UserAction {
    LOGIN("1"),
    CREATE("2"),
    VALIDATE("validate"),
    GENERATE("generate"),
    SAVE("save"),
    RETRIEVE("retrieve"),
    CHANGE("change"),
    DELETE("delete"),
    QUIT("quit"),
    MENU("menu"),
    TRY_AGAIN("try again"),
    PWNED("have i been pawned");

    private final String input;

    UserAction(String input) {
        this.input = input;
    }

    public static UserAction fromInput(String input) {
        for (UserAction action : values()) {
            if (action.input.equalsIgnoreCase(input.trim())) {
                return action;
            }
        }
        return null;
    }
}
