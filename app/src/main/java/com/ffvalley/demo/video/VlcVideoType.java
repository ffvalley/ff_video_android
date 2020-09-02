package com.ffvalley.demo.video;

import java.io.Serializable;

public enum VlcVideoType implements Serializable {
    // 1-本地视频、2-流媒体视频、3-资源视频
    LOCAL_VIDEO(1),
    RTSP_VIDEO(2),
    ASSET_VIDEO(3);

    private int value;

    private VlcVideoType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
