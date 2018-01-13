package com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser;

import android.net.Uri;
import com.google.gson.annotations.SerializedName;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;

import java.util.List;

public class PlayerResponseParser {
    private static final String PLAYER_RESPONSE_KEY = "player_response";
    private final String mContent;
    private final DocumentContext mParser;

    /**
     * Extracts subtitle, images and other objects from the <em>get_video_info</em> file
     * <br/>
     * For video object parsing use {@link com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.SimpleYouTubeInfoParser}
     * @param content get_video_info file content
     */
    public PlayerResponseParser(String content) {
        if (content == null) {
            throw new IllegalStateException("content cannot be null");
        }

        mContent = content;

        Uri videoInfo = Uri.parse("http://empty.url?" + content);
        String playerResponse = videoInfo.getQueryParameter(PLAYER_RESPONSE_KEY);

        Configuration conf = Configuration
                .builder()
                .mappingProvider(new GsonMappingProvider())
                .jsonProvider(new GsonJsonProvider())
                .build();

        mParser = JsonPath
                .using(conf)
                .parse(playerResponse);

    }

    public Uri getFirstSubsUri() {
        String url = mParser.read("$.captions.playerCaptionsTracklistRenderer.captionTracks[0].baseUrl");
        return Uri.parse(url);
    }

    public List<Subtitle> getAllSubs() {
        TypeRef<List<Subtitle>> typeRef = new TypeRef<List<Subtitle>>() {};
        return mParser.read("$.captions.playerCaptionsTracklistRenderer.captionTracks", typeRef);
    }

    public class Subtitle {
        @SerializedName("baseUrl")
        private String mBaseUrl;
        @SerializedName("isTranslatable")
        private boolean mIsTranslatable;
        @SerializedName("languageCode")
        private String mLanguageCode;
        @SerializedName("vssId")
        private String mVssId;
        @SerializedName("name")
        private Name mName;

        public String getBaseUrl() {
            return mBaseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            mBaseUrl = baseUrl;
        }

        public boolean isTranslatable() {
            return mIsTranslatable;
        }

        public void setTranslatable(boolean translatable) {
            mIsTranslatable = translatable;
        }

        public String getLanguageCode() {
            return mLanguageCode;
        }

        public void setLanguageCode(String languageCode) {
            mLanguageCode = languageCode;
        }

        public String getVssId() {
            return mVssId;
        }

        public void setVssId(String vssId) {
            mVssId = vssId;
        }

        public Name getName() {
            return mName;
        }

        public void setName(Name name) {
            mName = name;
        }

        private class Name {
            @SerializedName("simpleText")
            private String mSimpleText;

            public String getSimpleText() {
                return mSimpleText;
            }

            public void setSimpleText(String simpleText) {
                mSimpleText = simpleText;
            }
        }
    }
}
