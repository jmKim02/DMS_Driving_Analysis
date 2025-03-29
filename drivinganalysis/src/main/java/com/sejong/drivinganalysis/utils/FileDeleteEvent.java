package com.sejong.drivinganalysis.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileDeleteEvent {
    private final String filePath;
    private final Long videoId;
}