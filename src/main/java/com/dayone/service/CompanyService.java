package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistTickerException;
import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import com.dayone.scraper.YahooFinanceScraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    private final YahooFinanceScraper yahooFinanceScraper;

    public Company save(String ticker) {
        boolean result = companyRepository.existsByTicker(ticker);
        if(result){
            throw new AlreadyExistTickerException();
        }
        return storeCompanyAndDividend(ticker);
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        // 1. ticker 를 기준으로 회사를 스크래핑
        Company company = yahooFinanceScraper.scrapCompanyByTicker(ticker);
        // 2. 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        if (ObjectUtils.isEmpty(company)) {
            log.info("company found failed : " + company.toString());
            throw new NoCompanyException();
        }
        var result = yahooFinanceScraper.scrap(company);
        // 3. 스크래핑 결과 반환

        var entity = companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> list = result.getDividends().stream().map(d -> new DividendEntity(entity.getId(), d))
                .collect(Collectors.toList());
        dividendRepository.saveAll(list);
        return company;
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        Pageable pageable = PageRequest.of(0, 10);
        var list = companyRepository.findByNameStartingWithIgnoreCase(keyword, pageable);
        return list.stream().map(companyEntity -> companyEntity.getName()).collect(Collectors.toList());
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        // 1. 배당금 정보 삭제
        var company = companyRepository.findByTicker(ticker).orElseThrow(() -> new RuntimeException("ticker를 찾을 수 없습니다."));
        // 2. 회사 정보 삭제
        this.dividendRepository.deleteAllByCompanyId(company.getId());
        this.companyRepository.delete(company);

        this.deleteAutocompleteKeyword(company.getName());
        return company.getName();
    }

}
