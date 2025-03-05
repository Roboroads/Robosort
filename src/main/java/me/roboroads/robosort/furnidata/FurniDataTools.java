package me.roboroads.robosort.furnidata;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * Gracefully "yoinked" from G-Presets
 * <a href="https://github.com/sirjonasxx/G-Presets/blob/master/src/main/java/furnidata/FurniDataTools.java">Source</a>
 */
public class FurniDataTools {
    private static final Map<String, String> codeToDomainMap = new HashMap<>();

    static {
        codeToDomainMap.put("br", ".com.br");
        codeToDomainMap.put("de", ".de");
        codeToDomainMap.put("es", ".es");
        codeToDomainMap.put("fi", ".fi");
        codeToDomainMap.put("fr", ".fr");
        codeToDomainMap.put("it", ".it");
        codeToDomainMap.put("nl", ".nl");
        codeToDomainMap.put("tr", ".com.tr");
        codeToDomainMap.put("us", ".com");
    }

    private final String countryCode;
    private final Map<Integer, String> typeIdToNameFloor = new HashMap<>();
    private volatile boolean isReady = false;

    public FurniDataTools(String host) {
        countryCode = host.substring(5, 7);

        new Thread(() -> {
            try {
                fetch();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void fetch() throws IOException {
        JSONObject object = new JSONObject(IOUtils.toString(
          new URL(furnidataUrl()).openStream(), StandardCharsets.UTF_8));

        JSONArray floorJson = object.getJSONObject("roomitemtypes").getJSONArray("furnitype");
        floorJson.forEach(o -> {
            JSONObject item = (JSONObject) o;
            typeIdToNameFloor.put(item.getInt("id"), item.getString("classname"));
        });

        isReady = true;
    }

    private String furnidataUrl() {
        if (countryCode.equals("s2")) {
            return "https://sandbox.habbo.com/gamedata/furnidata_json/1";
        }

        return String.format("https://www.habbo%s/gamedata/furnidata_json/1", codeToDomainMap.get(countryCode));
    }

    public boolean isReady() {
        return isReady;
    }

    public String getFloorItemClassName(int typeId) {
        return typeIdToNameFloor.get(typeId);
    }

}
