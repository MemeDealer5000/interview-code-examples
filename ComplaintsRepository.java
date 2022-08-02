import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ComplaintsRepository implements IComplaintsRepository {
    final NamedParameterJdbcTemplate jdbcTemplate;
    final RowMapper<ComplaintsRow> complaintsMapper = new ComplaintsMapper();
    final RowMapper<UnsatisfiedRow> unsatisfiedMapper = new UnsatisfiedMapper();
    final RowMapper<GratitudeRow> gratitudeMapper = new GratitudeMapper();
    final RowMapper<EmployeeInfo> employeeInfoMapper = new EmployeeInfoMapper();
    final RowMapper<DictionaryInfo> dictionaryInfoMapper = new DictionaryInfoMapper();

    @Value("${select.unsatisfied.table}")
    String selectUnsatisfiedTable;

    @Value("${select.complaints.table}")
    String selectComplaintsTable;

    @Value("${refresh.complaints.table}")
    String refreshComplaintsTable;

    @Value("${refresh.unsatisfied.table}")
    String refreshUnsatisfiedTable;

    @Value("${select.gratitude.table}")
    String selectGratitudeTable;

    @Value("${select.employees.table}")
    String selectEmployeeInfo;

    @Value("${select.dictionary.info}")
    String selectDictionaryInfo;

    @Value("${select.dictionary.meta}")
    String selectDictionaryMeta;

    @Value("${select.column.aliases}")
    String selectAliases;

    public ComplaintsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ComplaintsRow> getComplaints(String periodStart, String periodEnd, String team) {
        String argString = StringUtils.getSqlArgument(List.of(periodStart, periodEnd, team));
        return jdbcTemplate.query(
                selectComplaintsTable,
                Map.of("arg", argString),
                complaintsMapper);
    }

    @Override
    public List<UnsatisfiedRow> getUnsatisfied(String periodStart, String periodEnd, String team) {
        String argString = StringUtils.getSqlArgument(List.of(periodStart, periodEnd, team));
        return jdbcTemplate.query(
                selectUnsatisfiedTable,
                Map.of("arg", argString),
                unsatisfiedMapper);
    }

    @Override
    public List<GratitudeRow> getGratitudes(String periodStart, String periodEnd){
        String arg = StringUtils.getSqlArgument(List.of(periodStart, periodEnd));
        return jdbcTemplate.query(
                selectGratitudeTable,
                Map.of("arg", arg),
                gratitudeMapper
        );
    }

    @Override
    public String refreshComplaints(String justification, String employeeCommentary, String verifier, String verifierCommentary, String reqNumber) {
        Map<String, String> typeValueMap = SqlArgumentDictionary.getComplaintsParamMap(
                justification,
                employeeCommentary,
                verifier,
                verifierCommentary);
        return refreshTableAndGetResult(typeValueMap, reqNumber);
    }

    @Override
    public String refreshUnsatisfied(String justification, String employeeCommentary, String verifier, String verifierCommentary, String reqNumber) {
        Map<String, String> typeValueMap = SqlArgumentDictionary.getComplaintsParamMap(
                justification,
                employeeCommentary,
                verifier,
                verifierCommentary);
        return refreshTableAndGetResult(typeValueMap, reqNumber);
    }

    @Override
    public String refreshGrade(String newGrade, String tabNum) {
        Map<String,String> paramMap = Map.of(SqlArgumentDictionary.GRADE, newGrade);
        return refreshTable(paramMap, tabNum).get(0);
    }

    @Override
    public String refreshGratitude(String gratitudeCode, String regDate, String initiator, String empTabNum, String comment){
        Map<String, String> typeValueMap = SqlArgumentDictionary.getGratitudeParamMap(
                gratitudeCode,
                regDate,
                initiator,
                empTabNum,
                comment);
        return refreshTableAndGetResult(typeValueMap,gratitudeCode);
    }

    @Override
    public List<EmployeeInfo> getEmployeeInfo() {
        return jdbcTemplate.query(selectEmployeeInfo, employeeInfoMapper);
    }

    @Override
    public String removeFromGratitude(String id){
        Map<String, String> typeValueMap =  SqlArgumentDictionary.getGratitudeParamMap(null, null, null, null, null);
        return refreshTableAndGetResult(typeValueMap, id);
    }

    @Override
    public String updateDictionaryMeta(String infoId, String infoValue) {
        return DataBaseUtils.updateDictionaryMeta(infoId,infoValue, jdbcTemplate);
    }

    @Override
    public DictionaryInfoResponse selectDictionaryInfo(String dictionaryKey) {
        Map<String, String> paramMap = Map.of("obj", dictionaryKey);
        List<String> columnNames = List.of(jdbcTemplate.query(selectAliases,
                        paramMap,
                        (resultSet, i) -> resultSet.getString("aa"))
                .get(0)
                .split(" :: "));
        List<DictionaryInfo> rows = jdbcTemplate.query(selectDictionaryInfo,
                paramMap,
                dictionaryInfoMapper);

        return new DictionaryInfoResponse(columnNames, rows);
    }

    @Override
    public List<DictionaryMeta> selectDictionaryMetaInfo() {
        return jdbcTemplate.query(selectDictionaryMeta, new DictionaryMetaMapper());
    }


    private String refreshTableAndGetResult(Map<String, String> typeValueMap, String reqNumber){
        List<String> updateResults = refreshTable(typeValueMap, reqNumber);
        int notGoodUpdates = (int) updateResults.stream().filter(x -> !x.equals("OK")).count();
        return notGoodUpdates == 0 ? "OK" : "Not ok";
    }

    private List<String> refreshTable(Map<String,String> typeValueMap, String reqNumber){
        List<String> updateResults = new ArrayList<>();
        for (Map.Entry<String,String> keyValue:
                typeValueMap.entrySet()) {
            updateResults.add(DataBaseUtils.callRefreshProcedure(keyValue.getKey(), reqNumber, keyValue.getValue(), jdbcTemplate));
        }
        return updateResults;
    }
}
