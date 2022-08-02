package spingboot.mvp.ru.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spingboot.mvp.ru.dictionary.LoggerObjects;
import spingboot.mvp.ru.dictionary.PeriodDictionary;
import spingboot.mvp.ru.dictionary.WorkGroupDictionary;
import spingboot.mvp.ru.model.comparison.ComparisonPeriod;
import spingboot.mvp.ru.model.comparison.PeriodComparisonData;
import spingboot.mvp.ru.model.SpecialistCardData;
import spingboot.mvp.ru.service.ISpecialistCardService;
import spingboot.mvp.ru.utils.StringUtils;
import spingboot.mvp.ru.utils.logger.TypeCLoggerUtils;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialistCard")
public class SpecialistCardController {
    private final ISpecialistCardService specialistCardService;
    private final TypeCLoggerUtils logger;

    public SpecialistCardController(ISpecialistCardService specialistCardService,
                                    AuditCTypeLogger auditLogger){
        this.specialistCardService = specialistCardService;
        this.logger = new TypeCLoggerUtils(auditLogger);
    }

    @GetMapping("/cards")
    @Operation(
            summary = "--PLACEHOLDER--",
            description = "--PLACEHOLDER--",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Запрос выполнен успешно",
                            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SpecialistCardData.class)))),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Ошибка аутентификации",
                            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ExceptionResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Ошибка при выполнении запроса",
                            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ExceptionResponse.class)))}
    )
    public ResponseEntity<List<SpecialistCardData>> getSpecialistCards(
            @RequestParam(value = StringUtils.PERIOD) String period,
            @RequestParam(value = StringUtils.TEAM) String team,
            @RequestParam(value = StringUtils.YEAR) String year,
            @RequestParam(value = StringUtils.TABEL_NUMBER, required = false) String tabNum
    ){
        String objName = LoggerObjects.SPECIALIST_CARD_OBJ.getName();
        String objId = String.valueOf(LoggerObjects.SPECIALIST_CARD_OBJ.getId());
        String actualTeam = WorkGroupDictionary.getTeamByShortName(team);
        if(PeriodDictionary.isPeriodValid(period) && !actualTeam.isBlank()){
            logger.sendDataRequestMessage(objName, objId);
            Integer actualYear = Integer.parseInt(year);
            String actualTabNum = tabNum == null ? "":tabNum;
            List<SpecialistCardData> specialistCardDataList = specialistCardService.getCards(period, actualTeam, actualYear, actualTabNum);
            logger.sendDataGatheredMessage(objName, objId);
            return ResponseEntity.ok(specialistCardDataList);
        }
        return ResponseEntity.badRequest().build();
    }


    @GetMapping("/periodComparison")
    @Operation(
            summary = "--PLACEHOLDER--",
            description = "--PLACEHOLDER--",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Запрос выполнен успешно",
                            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SpecialistCardData.class)))),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Ошибка аутентификации",
                            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ExceptionResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Ошибка при выполнении запроса",
                            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ExceptionResponse.class)))}
    )
    public ResponseEntity<List<PeriodComparisonData>> getPeriodComparison(
            @RequestParam(value = StringUtils.FIRST_PERIOD) String firstPeriod,
            @RequestParam(value = StringUtils.SECOND_PERIOD) String secondPeriod,
            @RequestParam(value = StringUtils.TEAM) String workGroup
    ){
        String objName = LoggerObjects.PERIOD_COMPARISON_OBJ.getName();
        String objId = String.valueOf(LoggerObjects.PERIOD_COMPARISON_OBJ.getId());
        String actualTeam = WorkGroupDictionary.getTeamByShortName(workGroup);
        if(!actualTeam.isBlank()){
            logger.sendDataRequestMessage(objName, objId);

            String[] firstPeriodParsed = firstPeriod.split(":");
            String[] secondPeriodParsed = secondPeriod.split(":");
            ComparisonPeriod first = ComparisonPeriod.build(firstPeriodParsed[0], firstPeriodParsed[1]);
            ComparisonPeriod second = ComparisonPeriod.build(secondPeriodParsed[0], secondPeriodParsed[1]);

            List<PeriodComparisonData> comparisonData = specialistCardService.getComparisonData(first, second,actualTeam);
            logger.sendDataGatheredMessage(objName, objId);
            return ResponseEntity.ok(comparisonData);
        }
        return ResponseEntity.badRequest().build();
    }
}
