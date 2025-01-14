package com.cat2bug.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCaseStep;
import com.cat2bug.system.domain.vo.ExcelImportResultVo;
import com.cat2bug.system.domain.vo.ExcelImportRowResultVo;
import com.cat2bug.system.mapper.SysModuleMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.service.ISysCaseService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试用例Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Service
@Log4j2
public class SysCaseServiceImpl implements ISysCaseService 
{
    @Autowired
    private SysCaseMapper sysCaseMapper;

    @Autowired
    private SysModuleMapper sysModuleMapper;

    /**
     * 查询测试用例
     * 
     * @param caseId 测试用例主键
     * @return 测试用例
     */
    @Override
    public SysCase selectSysCaseByCaseId(Long caseId)
    {
        return sysCaseMapper.selectSysCaseByCaseId(caseId);
    }

    @Override
    public SysCase selectSysCaseByCaseName(Long projectId, String caseName) {
        return sysCaseMapper.selectSysCaseByCaseName(projectId,caseName);
    }

    /**
     * 查询测试用例列表
     * 
     * @param sysCase 测试用例
     * @return 测试用例
     */
    @Override
    public List<SysCase> selectSysCaseList(SysCase sysCase)
    {
        return sysCaseMapper.selectSysCaseList(sysCase);
    }

    /**
     * 新增测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    @Override
    public SysCase insertSysCase(SysCase sysCase)
    {
        long count = sysCaseMapper.getCaseMaxNumOfProject(sysCase.getProjectId());
        sysCase.setCaseNum(count+1);
        sysCase.setCreateById(SecurityUtils.getUserId());
        sysCase.setCreateTime(DateUtils.getNowDate());
        sysCase.setUpdateById(SecurityUtils.getUserId());
        sysCase.setUpdateTime(DateUtils.getNowDate());
        sysCaseMapper.insertSysCase(sysCase);
        return sysCase;
    }

    @Override
    public List<SysCase> batchInsertSysCase(List<SysCase> list) {
        for(SysCase sc : list){
            long count = sysCaseMapper.getCaseMaxNumOfProject(sc.getProjectId());
            sc.setCaseNum(count+1);
            sc.setCreateById(SecurityUtils.getUserId());
            sc.setCreateTime(DateUtils.getNowDate());
            sc.setUpdateById(SecurityUtils.getUserId());
            sc.setUpdateTime(DateUtils.getNowDate());
            sysCaseMapper.insertSysCase(sc);
        }
        return list;
    }

    /**
     * 修改测试用例
     * 
     * @param sysCase 测试用例
     * @return 结果
     */
    @Override
    public int updateSysCase(SysCase sysCase)
    {
        sysCase.setUpdateTime(DateUtils.getNowDate());
        return sysCaseMapper.updateSysCase(sysCase);
    }

    /**
     * 批量删除测试用例
     * 
     * @param caseIds 需要删除的测试用例主键
     * @return 结果
     */
    @Override
    public int deleteSysCaseByCaseIds(Long[] caseIds)
    {
        return sysCaseMapper.deleteSysCaseByCaseIds(caseIds);
    }

    /**
     * 删除测试用例信息
     * 
     * @param caseId 测试用例主键
     * @return 结果
     */
    @Override
    public int deleteSysCaseByCaseId(Long caseId)
    {
        return sysCaseMapper.deleteSysCaseByCaseId(caseId);
    }

    /**
     * 导入用例数据
     *
     * @param caseList 用例数据列表
     * @param projectId 项目id
     * @return 结果
     */
    @Override
    @Transactional
    public ExcelImportResultVo importCase(List<SysCase> caseList, Long projectId)
    {
        if (StringUtils.isNull(caseList) || caseList.size() == 0)
        {
            throw new ServiceException(MessageUtils.message("case.import-data-not-empty"));
        }
        List<ExcelImportRowResultVo> rows = new ArrayList<>();

        int rowNum = 2;
        for (SysCase c : caseList)
        {
            ExcelImportRowResultVo rr = new ExcelImportRowResultVo();
            rr.setRowNum(rowNum);
            rr.setMessages(new ArrayList<>());
            if(c==null) {
                rr.getMessages().add(MessageUtils.message("case.row-is-empty"));
                rows.add(rr);
                continue;
            }

            if(StringUtils.isEmpty(c.getCaseName())) {
                rr.getMessages().add(MessageUtils.message("case.title-not-empty"));
            } else if(c.getCaseName().length()>255) {
                rr.getMessages().add(MessageUtils.message("case.title-size-exception"));
            }
            if(StringUtils.isEmpty(c.getCaseExpect())) {
                rr.getMessages().add(MessageUtils.message("case.expect-not-empty"));
            } else if(c.getCaseExpect().length()>255) {
                rr.getMessages().add(MessageUtils.message("case.expect-size-exception"));
            }
            if(rr.getMessages().size()>0){
                rows.add(rr);
                continue;
            }
            rowNum++;
        }

        if(rows.size()==0) {
            long num = sysCaseMapper.getCaseMaxNumOfProject(projectId);
            for (SysCase c : caseList) {
                c.setCaseNum(++num);
                c.setProjectId(projectId);
                c.setCreateById(SecurityUtils.getUserId());
                c.setCreateTime(DateUtils.getNowDate());
                c.setUpdateById(SecurityUtils.getUserId());
                c.setUpdateTime(DateUtils.getNowDate());
                if(StringUtils.isNotBlank(c.getModuleName())) {
                    c.setModuleId(Long.parseLong(c.getModuleName()));
                }
                if(StringUtils.isNotBlank(c.getCaseStepScript())) {
                    String[] rowSteps = c.getCaseStepScript().toString().split("\n");
                    List<SysCaseStep> steps =  Arrays.stream(rowSteps).map(s->{
                        SysCaseStep cs = new SysCaseStep();
                        String[] parasm = s.split("---");
                        cs.setStepDescribe(parasm[0]);
                        if(parasm.length>1){
                            cs.setStepExpect(parasm[1]);
                        }
                        return cs;
                    }).collect(Collectors.toList());
                    c.setCaseStep(steps);
                }
                sysCaseMapper.insertSysCase(c);
            }
        }

        ExcelImportResultVo ret = new ExcelImportResultVo();
        ret.setRows(rows);
        if (rows.size() > 0)
        {
            ret.setMessage(MessageUtils.message("case.import-exception", rows.size()));
        }
        else
        {
            ret.setMessage(MessageUtils.message("case.import-success"));
        }
        return ret;
    }
}
