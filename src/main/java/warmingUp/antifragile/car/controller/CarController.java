package warmingUp.antifragile.car.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import warmingUp.antifragile.car.domain.Model;
import warmingUp.antifragile.car.dto.RecommendCarDto;
import warmingUp.antifragile.car.dto.RecommendDto;
import warmingUp.antifragile.car.repository.ModelRepository;

import java.util.*;

@RestController
public class CarController {

    @Autowired
    private ModelRepository modelRepository;

    // 전체 모델 랭킹 반환 요청 처리 (키워드 점수 높은 순 - 리뷰 많은 순 - 모델명 순)
    @GetMapping("/reviews/ranking")
    public ArrayList<Model> getAllRanking(@RequestParam(value = "keyword", required = false) String keyword,
                                          @RequestParam(value = "minPrice", required = false) Integer minPrice,
                                          @RequestParam(value = "maxPrice", required = false) Integer maxPrice) {

        if(minPrice == null)
            minPrice = 0;
        if(maxPrice == null)
            maxPrice = Integer.MAX_VALUE;
        // 해당 가격 범위의 모델 목록 가져오기
        ArrayList<Model> list = modelRepository.findByPriceBetween(minPrice, maxPrice);

        // 만약 키워드 필터링이 적용되지 않았을 경우 -> 총 5개의 우선 고려사항 평균 점수 합산 높은 순 - 리뷰 많은 순 - 모델명 순 정렬
        if(keyword == null) {
            return noKeywordSort(list);
        }
        // 키워드 필터링 적용 -> 키워드 점수 높은 순 - 리뷰 많은 순 - 모델명 순
        return keywordSort(keyword, list);
    }

    // 키워드 필터가 적용되지 않음 -> 전체 키워드 평균 합산 높은 순 - 리뷰 많은 순 - 이름 순으로 정렬
    public ArrayList<Model> noKeywordSort(ArrayList<Model> list) {
        Collections.sort(list, new Comparator<Model>() {
            @Override
            public int compare(Model m1, Model m2) {
                // 1. 키워드 점수 높은 순
                if(m1.allKeywordAvg() > m2.allKeywordAvg())
                    return -1;
                else if(m1.allKeywordAvg() < m2.allKeywordAvg())
                    return 1;
                else {      // 2. 리뷰 많은 순
                    if(m1.getReviewCount() > m2.getReviewCount())
                        return -1;
                    else if(m1.getReviewCount() < m2.getReviewCount())
                        return 1;
                    else {      // 3. 이름 순
                        return m1.getName().compareTo(m2.getName());
                    }
                }
            }
        });
        return list;
    }
    // 정해진 키워드 기준으로 점수 높은 순 - 리뷰 많은 순 - 이름 순 정렬
    public ArrayList<Model> keywordSort(String keyword, ArrayList<Model> list) {
        Collections.sort(list, new Comparator<Model>() {
            @Override
            public int compare(Model m1, Model m2) {
                // 1. 키워드 점수 높은 순
                if(m1.keywordAvg(keyword) > m2.keywordAvg(keyword))
                    return -1;
                else if(m1.keywordAvg(keyword) < m2.keywordAvg(keyword))
                    return 1;
                else {      // 2. 리뷰 많은 순
                    if(m1.getReviewCount() > m2.getReviewCount())
                        return -1;
                    else if(m1.getReviewCount() < m2.getReviewCount())
                        return 1;
                    else {      // 3. 이름 순
                        return m1.getName().compareTo(m2.getName());
                    }
                }
            }
        });
        return list;
    }

    // 해당 모델의 상세 설명을 볼 수 있는 URL 링크 반환
    @GetMapping("/recommend/{modelId}")
    public String getURL(@PathVariable Long modelId) {
        Model model = modelRepository.findById(modelId).orElse(null);
        if(model == null)
            return "해당하는 모델이 없습니다.";
        else
            return model.getInformationURL();
    }
    // 유저의 선택 기반 추천 모델 2가지 반환
    // 인원수 -> 예산 범위 -> 우선 고려 사항 점수 -> 주요 이용 목적
    @PostMapping("/recommend")
    public ArrayList<RecommendCarDto> recommend(@RequestBody RecommendDto recommendDto) {


        List<Model> models = modelRepository.findAll();
        Integer minPrice = recommendDto.getMinPrice();
        Integer maxPrice = recommendDto.getMaxPrice();
        if(minPrice == null)
            minPrice = 0;
        if(maxPrice == null)
            maxPrice = 6000;

        int wantedPrice = (minPrice + maxPrice)/2;
        int wantedPeaple = recommendDto.getAdultsCount()+recommendDto.getKidsCount();

        for(Model m: models){
            //가격점수 클수록 나쁨
            Integer score = Math.abs((m.getPrice()-wantedPrice)/100 /5);
            m.setPrice(score);
            //인구수 점수 클수록 나쁨
            Integer peopleScore = Math.abs(m.getPeople()-wantedPeaple);
            m.setPeople(peopleScore);

            //항목별 점수
            Long cnt = m.getReviewCount();
            m.setMpgSum(m.getMpgSum()/cnt/3);
            m.setSafeSum(m.getSafeSum()/cnt/3);
            m.setSpaceSum(m.getSpaceSum()/cnt/3);
            m.setDesignSum(m.getDesignSum()/cnt/3);
            m.setFunSum(m.getFunSum()/cnt/3);

            //우선순위 퍼센테지화
            m.setWorkCount(m.getWorkCount()*100/cnt);
            m.setDriveCount(m.getDriveCount()*100/cnt);
            m.setLongCount(m.getLongCount()*100/cnt);
            m.setKidsCount(m.getKidsCount()*100/cnt);
            m.setTravelCount(m.getTravelCount()*100/cnt);

        }

        //가격별 정렬
        //인구수 정렬

        models.sort(((o1, o2) -> o2.getPrice()-o1.getPrice()));
        models.sort(((o1, o2) -> o2.getPeople()-o1.getPeople()));

        String p1 = recommendDto.getPriority1();
        String p2 = recommendDto.getPriority2();

        //우선순위별 정렬
        for(Model m: models){
            Long k = 0L;
            if(p1.equals("연비")){
                k += m.getMpgSum();
            }
            else if(p1.equals("승차감 및 안전")){
                k+= m.getSafeSum();
            }
            else if(p1.equals("넓은 공")){
                k+= m.getSpaceSum();
            }else if(p1.equals("디자인")){
                k+= m.getDesignSum();
            }else if(p1.equals("운전 재미")){
                k+= m.getFunSum();
            }

            if(p2.equals("연비")){
                k += m.getMpgSum();
            }
            else if(p2.equals("승차감 및 안전")){
                k+= m.getSafeSum();
            }
            else if(p2.equals("넓은 공")){
                k+= m.getSpaceSum();
            }else if(p2.equals("디자인")){
                k+= m.getDesignSum();
            }else if(p2.equals("운전 재미")){
                k+= m.getFunSum();
            }
            m.setReviewCount(k);
        }
        models.sort((o1, o2) -> Long.compare(o2.getReviewCount(), o1.getReviewCount()));

        //이용목적정렬

        String purpose = recommendDto.getPurpose();
        if (purpose.equals("출퇴근용"))
            models.sort((o1, o2) -> Long.compare(o2.getWorkCount(), o1.getWorkCount()));

        else if(purpose.equals("장거리 운전"))
            models.sort((o1, o2) -> Long.compare(o2.getLongCount(), o1.getLongCount()));
        else if(purpose.equals("드라이브"))
            models.sort((o1, o2) -> Long.compare(o2.getDriveCount(), o1.getDriveCount()));
        else if(purpose.equals("주말여행"))
            models.sort((o1, o2) -> Long.compare(o2.getTravelCount(), o1.getTravelCount()));
        else if(purpose.equals("자녀와 함께"))
            models.sort((o1, o2) -> Long.compare(o2.getKidsCount(), o1.getKidsCount()));


        Long firstId = models.get(0).getId();
        Long secondId = models.get(1).getId();
        Model m1 = modelRepository.findById(firstId).orElse(null);
        Model m2 = modelRepository.findById(secondId).orElse(null);

        String m1Pri = m1.bestPriority();
        int m1PriPercent = (int)m1.priorityPercent(m1Pri);
        String m2Pri = m2.bestPriority();
        int m2PriPercent = (int)m2.priorityPercent(m2Pri);
        String m1Pur = m1.bestPurpose();
        int m1PurPercent = (int)m1.purposePercent(m1Pur);
        String m2Pur = m2.bestPurpose();
        int m2PurPercent = (int)m2.purposePercent(m2Pur);


        RecommendCarDto car1 = new RecommendCarDto(m1.getId(), m1.getName(), m1.getDescription(),
                m1.getInformationURL(), m1.getPrice(), m1Pri, m1PriPercent, m1Pur, m1PurPercent);
        RecommendCarDto car2 = new RecommendCarDto(m2.getId(), m2.getName(), m2.getDescription(),
                m2.getInformationURL(), m2.getPrice(), m2Pri, m2PriPercent, m2Pur, m2PurPercent);
        ArrayList<RecommendCarDto> result = new ArrayList<>();
        result.add(car1);
        result.add(car2);
        return result;



    }

    // ArrayList<Model> 정렬 함수
    // 인원수 적은 순 - 우선 고려 사항 점수 높은 순 - 주요 이용 목적 점수 높은 순 - 저렴한 순 - 이름 순
    public ArrayList<Model> sortSmallPeople(String p1, String p2, String purpose, ArrayList<Model> list) {
        Collections.sort(list, new Comparator<Model>() {
            @Override
            public int compare(Model m1, Model m2) {
                // 1. 인원수 적은 순
                if (m1.getPeople() < m2.getPeople())
                    return -1;
                else if (m1.getPeople() > m2.getPeople())
                    return 1;
                else {
                    // 2. 우선 고려 사항 점수 높은 순
                    if (m1.twoPriorityScore(p1, p2) > m2.twoPriorityScore(p1, p2))
                        return -1;
                    else if (m1.twoPriorityScore(p1, p2) < m2.twoPriorityScore(p1, p2))
                        return 1;
                    else {
                        // 3. 주요 이용 목적 점수 높은 순
                        if (m1.purposePercent(purpose) > m2.purposePercent(purpose))
                            return -1;
                        else if (m1.purposePercent(purpose) < m2.purposePercent(purpose))
                            return 1;
                        else {
                            // 4. 저렴한 순
                            if (m1.getPrice() < m2.getPrice())
                                return -1;
                            else if (m1.getPrice() > m2.getPrice())
                                return 1;
                            else {
                                // 5. 이름 순
                                return m1.getName().compareTo(m2.getName());
                            }
                        }
                    }
                }
            }
        });
        return list;
    }

    // ArrayList<Model> 정렬 함수
    // 인원수 많은 순 - 우선 고려 사항 점수 높은 순 - 주요 이용 목적 점수 높은 순 - 저렴한 순 - 이름 순
    public ArrayList<Model> sortBigPeople(String p1, String p2, String purpose, ArrayList<Model> list) {
        Collections.sort(list, new Comparator<Model>() {
            @Override
            public int compare(Model m1, Model m2) {
                // 1. 인원수 많은 순
                if (m1.getPeople() > m2.getPeople())
                    return -1;
                else if (m1.getPeople() < m2.getPeople())
                    return 1;
                else {
                    // 2. 우선 고려 사항 점수 높은 순
                    if (m1.twoPriorityScore(p1, p2) > m2.twoPriorityScore(p1, p2))
                        return -1;
                    else if (m1.twoPriorityScore(p1, p2) < m2.twoPriorityScore(p1, p2))
                        return 1;
                    else {
                        // 3. 주요 이용 목적 점수 높은 순
                        if (m1.purposePercent(purpose) > m2.purposePercent(purpose))
                            return -1;
                        else if (m1.purposePercent(purpose) < m2.purposePercent(purpose))
                            return 1;
                        else {
                            // 4. 저렴한 순
                            if (m1.getPrice() < m2.getPrice())
                                return -1;
                            else if (m1.getPrice() > m2.getPrice())
                                return 1;
                            else {
                                // 5. 이름 순
                                return m1.getName().compareTo(m2.getName());
                            }
                        }
                    }
                }
            }
        });
        return list;
    }
}
