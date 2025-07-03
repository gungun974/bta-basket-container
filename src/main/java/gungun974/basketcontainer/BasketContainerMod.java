package gungun974.basketcontainer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasketContainerMod implements ModInitializer   {
    public static final String MOD_ID = "basketContainer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("BasketContainerMod initialized.");
    }
}
