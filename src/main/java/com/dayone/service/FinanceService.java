package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;
    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        log.info("Search company : " + companyName);
        // 1. 회사명을 기준으로 회사 정보를 조회
        CompanyEntity company =  companyRepository.findByName(companyName).orElseThrow(NoCompanyException::new);
        // 2. 조회된 회사 ID 로 배당금 정보 조회
        var dividendEntities =  dividendRepository.findAllByCompanyId(company.getId());
        // 3. 결과 조합 후 반환

        var list = dividendEntities.stream().map(d ->Dividend.builder().dividend(d.getDividend()).date(d.getDate()).build())
                .collect(Collectors.toList());
        return new ScrapedResult(Company.builder().ticker(company.toString())
                .name(company.getName()).build(), list);
    }
}
