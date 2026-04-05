package com.iwip.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件视图对象 sys_file
 *
 * @author iwip
 */
@Data
public class SysFileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件后缀名
     */
    private String originalName;

    /**
     * URL地址
     */
    private String url;

    /**
     * 文件大小
     */
    private Long size;

}
