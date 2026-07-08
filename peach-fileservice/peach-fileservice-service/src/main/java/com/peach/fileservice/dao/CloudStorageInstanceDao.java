package com.peach.fileservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.fileservice.entity.CloudStorageInstanceDO;
import com.peach.fileservice.qo.CloudStorageInstanceQO;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 云存储实例数据访问接口.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Indexed
@MybatisDao
public interface CloudStorageInstanceDao extends PeachDao<CloudStorageInstanceDO, CloudStorageInstanceVO> {
    List<CloudStorageInstanceVO> selectByQO(CloudStorageInstanceQO qo);
}
