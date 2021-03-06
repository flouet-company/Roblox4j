package online.pizzacrust.roblox;

/**
 * Represents an entity that has a profile on ROBLOX.com.
 *
 * @since 1.0-SNAPSHOT
 * @author PizzaCrust
 */
public interface Profile {

    /**
     * Retrieves the numerical Id for the profile.
     * @return
     */
    int getUserId();

    /**
     * Retrieves the current username of the profile.
     * @return
     */
    String getUsername();

}
