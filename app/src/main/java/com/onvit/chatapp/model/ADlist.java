package com.onvit.chatapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ADlist implements Parcelable {
    private String thumbnail;
    private String original;

    public ADlist() {
    }

    protected ADlist(Parcel in) {
        thumbnail = in.readString();
        original = in.readString();
    }

    public static final Creator<ADlist> CREATOR = new Creator<ADlist>() {
        @Override
        public ADlist createFromParcel(Parcel in) {
            return new ADlist(in);
        }

        @Override
        public ADlist[] newArray(int size) {
            return new ADlist[size];
        }
    };

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    @Override
    public String toString() {
        return "ADlist{" +
                "thumbnail='" + thumbnail + '\'' +
                ", original='" + original + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(thumbnail);
        parcel.writeString(original);
    }
}
