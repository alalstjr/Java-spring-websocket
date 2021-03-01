package com.jjunpro.website.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutputMessage {

    private String from;
    private String text;
    private String time;

    public OutputMessage(final String from, final String text, final String time) {

        this.from = from;
        this.text = text;
        this.time = time;
    }
}