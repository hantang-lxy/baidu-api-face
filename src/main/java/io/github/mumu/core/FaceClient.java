package io.github.mumu.core;

import com.baidu.aip.face.AipFace;
import com.baidu.aip.face.MatchRequest;
import org.json.JSONObject;

import java.util.*;

/**
 * 调用百度人脸识别api，用于人脸登录、注册。
 *
 * @author mumu
 * @date 2020/1/23
 */
public class FaceClient {

    private FaceClient() {}

    // 常用百度api返回的常用json属性
    private static final String FACE_TOKEN = "face_token";
    private static final String USER_ID = "user_id";
    private static final String USER_LIST = "user_list";
    private static final String SCORE = "score";
    private static final String ERR_MSG = "error_msg";
    private static final String ERR_CODE = "error_code";
    private static final String RESULT = "result";

    //  百度api错误信息头
    private final String ERROR_MSG_HEADER = "百度api错误:";

    private AipFace client;

    // 默认分数，两个人脸数据匹配成绩达到该分数即为同一张脸。
    private final Float DEFAULT_SCORE = 92f;

    private static FaceClient instance = new FaceClient();

    // 将错误代码与错误信息对应
    private static Map<String, String> MSG_MAP = new HashMap<>();

    static {
        MSG_MAP.put("222202", "图片中没有人脸");
        MSG_MAP.put("222203", "无法解析人脸");
    }

    public FaceClient config(String appId, String apiKey, String secretKey) {
        this.client = new AipFace(appId, apiKey, secretKey);
        return this;
    }

    public static FaceClient newInstance(String appId, String apiKey, String secretKey) {
        return instance.config(appId, apiKey, secretKey);
    }

    /**
     * 在人脸库中进行搜索，取结果列表中的第一个结果。
     *
     * @param base64 图片数据经过Base64编码后的字符串
     * @return
     */
    public JSONObject search(String base64, String imageType, String group, HashMap<String, String> options) {
        JSONObject jsonObject = client.search(base64, imageType, group, options);
        return Optional.ofNullable(jsonObject)
                .map(object -> check(object))
                .map(integer -> jsonObject.has(RESULT))
                .map(b -> b ? jsonObject.getJSONObject(RESULT) : null)
                .map(object -> object.getJSONArray(USER_LIST))
                .map(array -> array.getJSONObject(0))
                .orElseThrow(() -> new FaceClientException("没有匹配到结果，你可能没有人脸注册。"));
    }

    /**
     * 获取百度api返回的json中的匹配分数，并与设置的分数线比较。
     *
     * @param object
     * @param score
     * @return
     */
    public boolean verify(JSONObject object, float score) {
        return Float.parseFloat(getString(object, SCORE)) - score >= 0;
    }

    public boolean verify(JSONObject object) {
        return verify(object, DEFAULT_SCORE);
    }

    /**
     * 获取百度api返回的json中的某一字段的值（String类型）。该函数能进行null检查。
     *
     * @param object
     * @param field
     * @return
     */
    public String getString(JSONObject object, String field) {
        return String.valueOf(
                Optional.ofNullable(object)
                        .map(o -> o.has(field))
                        .map(b -> b ? object.get(field) : null)
                        .orElseThrow(() -> new FaceClientException("无法从json中获取" + field)));
    }

    public boolean match(String imgStr1, String imgStr2, String imageType) {
        return match(imgStr1, imgStr2, imageType, DEFAULT_SCORE);
    }

    /**
     * 使用百度api进行人脸图片比较
     *
     * @param imgStr1
     * @param imgStr2
     * @param imageType
     * @return
     */
    public boolean match(String imgStr1, String imgStr2, String imageType, float score) {
        MatchRequest req1 = new MatchRequest(imgStr1, imageType);
        MatchRequest req2 = new MatchRequest(imgStr2, imageType);

        List<MatchRequest> requests = new ArrayList<MatchRequest>();
        requests.add(req1);
        requests.add(req2);

        JSONObject res = client.match(requests);

        return verify(res, score);
    }

    /**
     * 向百度api添加人脸数据，即完成人脸注册。
     *
     * @param id     用于百度人脸库的中的user_id
     * @param base64 图片数据经过Base64编码后的字符串
     * @return 百度api返回的face_token
     */
    public String addFace(Integer id, String base64, String imageType, String group, HashMap<String, String> options) {
        JSONObject res = client.addUser(base64, imageType, group, String.valueOf(id), options);
        return getString(check(res), FACE_TOKEN);
    }

    /**
     * 检查百度api是否出错，出错则抛出异常。
     *
     * @param jsonObject 百度api返回的json
     */
    private JSONObject check(JSONObject jsonObject) {
        return Optional.ofNullable(jsonObject)
                .map(object -> object.has(ERR_CODE))
                .map(b -> b ? jsonObject.getInt(ERR_CODE) : -1)
                .map(integer -> integer != 0 ? null : jsonObject)
                .orElseThrow(() ->
                        new FaceClientException(ERROR_MSG_HEADER + MSG_MAP.get(getString(jsonObject, ERR_CODE))));
    }
}
