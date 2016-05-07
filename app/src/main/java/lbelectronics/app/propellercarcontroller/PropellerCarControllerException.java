package lbelectronics.app.propellercarcontroller;

/**
 * Created by philipp on 02.05.15.
 */
public class PropellerCarControllerException extends Exception {

    public PropellerCarControllerException(String message)
    {
        super(message);
    }

    public PropellerCarControllerException()
    {
        super("Unknown reason of error");
    }
}
