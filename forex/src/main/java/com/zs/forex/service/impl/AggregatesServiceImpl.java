package com.zs.forex.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zs.forex.common.dto.AggregatesDTO;
import com.zs.forex.common.dto.ParseDTO;
import com.zs.forex.common.dto.QuoteDTO;
import com.zs.forex.common.dto.StrategyDTO;
import com.zs.forex.common.pojo.Symbol;
import com.zs.forex.common.tools.FormulaTool;
import com.zs.forex.common.vcenum.LevelType;
import com.zs.forex.common.vcenum.SymbolType;
import com.zs.forex.handler.AggregateHandler;

import com.zs.forex.handler.CoreHandler;
import com.zs.forex.handler.SelfCoinHandler;
import com.zs.forex.market.PolygonCrpytoRestApi;
import com.zs.forex.market.PolygonForexRestApi;
import com.zs.forex.market.PolygonIndexRestApi;
import com.zs.forex.service.AggregatesService;
import com.zs.forex.service.SymbolService;
import com.zs.forex.task.AggregateTask;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AggregatesServiceImpl implements AggregatesService {

    private final JdbcTemplate readJdbcTemplate;

    private final JdbcTemplate writeJdbcTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final PolygonForexRestApi polygonForexRestApi;

    private final PolygonIndexRestApi polygonIndexRestApi;

    private final PolygonCrpytoRestApi polygonCrpytoRestApi;


    @Lazy
    @Autowired
    private SymbolService symbolService;

    public AggregatesServiceImpl(JdbcTemplate readJdbcTemplate, JdbcTemplate writeJdbcTemplate,
                                 StringRedisTemplate stringRedisTemplate, PolygonForexRestApi polygonForexRestApi,
                                 PolygonIndexRestApi polygonIndexRestApi, PolygonCrpytoRestApi polygonCrpytoRestApi
                                 ) {
        this.readJdbcTemplate = readJdbcTemplate;
        this.writeJdbcTemplate = writeJdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.polygonForexRestApi = polygonForexRestApi;
        this.polygonIndexRestApi = polygonIndexRestApi;
        this.polygonCrpytoRestApi = polygonCrpytoRestApi;
    }

    private static final List<AggregateTask> taskList = new ArrayList<>();

    @Value("${nats.url}")
    private String url;
    private Connection connect;
    private Dispatcher dispatcher;

    @PostConstruct
    public void init() {
        Options o = new Options.Builder().maxMessagesInOutgoingQueue(200000000).connectionName("AggregatesService").server(url).maxReconnects(-1).build();
        try {
            connect = Nats.connect(o);
            dispatcher = connect.createDispatcher();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void generateTableAll(Symbol symbol) {

        this.generateTable(symbol, LevelType.minute, 1);
        this.generateTable(symbol, LevelType.minute, 5);
        this.generateTable(symbol, LevelType.minute, 15);
        this.generateTable(symbol, LevelType.minute, 30);
        this.generateTable(symbol, LevelType.hour, 1);
        this.generateTable(symbol, LevelType.day, 1);


    }

    @Override
    public void generateTable(Symbol symbol, LevelType levelType, Integer grade) {
        String name = this.tableName(symbol.getCode(), levelType, grade);
        boolean existTable = this.existTable(name);
        if (!existTable) {
            this.createTable(name);
            if (symbol.getHasSelf() == 0) {
                this.synchronousData(symbol, 350, levelType, grade);
            }
        }
    }

    @Override
    public void synchronousData(Symbol symbol, Integer limit, LevelType levelType, Integer grade) {
        log.warn("synchronousData symbol:{},limit:{},levelType:{},grade:{}", symbol, limit, levelType, grade);
        Integer type = symbol.getType();
        List<AggregatesDTO> data = null;
        String startTime = DateUtil.formatDate(DateUtil.offset(new Date(), DateField.YEAR, -1));
        String endTime = DateUtil.formatDate(new Date());
        if (type == SymbolType.Forex.ordinal() || type == SymbolType.Metal.ordinal()) {
            data = polygonForexRestApi.aggregatesDTOList(symbol.getCode(), levelType.name(), startTime, endTime, limit, grade);
            log.warn("synchronousData Forex data:{}", data);
        } else if (type == SymbolType.Index.ordinal()) {
            data = polygonIndexRestApi.aggregatesDTOList(symbol.getBase(), levelType.name(), startTime, endTime, limit, grade);
            log.warn("synchronousData Index data:{}", data);
        } else if (type == SymbolType.Crypto.ordinal()) {
            data = polygonCrpytoRestApi.aggregatesDTOList(symbol.getCode(), levelType.name(), startTime, endTime, limit, grade);
            log.warn("synchronousData Index Crypto:{}", data);

        }

        if (data != null && !data.isEmpty()) {
            Integer precision = symbolService.getPrecision(symbol.getCode());
            //处理小数位
            data.forEach(item -> {
                item.setH(item.getH().setScale(precision, RoundingMode.HALF_UP));
                item.setL(item.getL().setScale(precision, RoundingMode.HALF_UP));
                item.setO(item.getO().setScale(precision, RoundingMode.HALF_UP));
                item.setC(item.getC().setScale(precision, RoundingMode.HALF_UP));
            });
            String name = this.tableName(symbol.getCode(), levelType, grade);
            if (levelType == LevelType.day) {
                data.forEach(item -> item.setT(DateUtil.beginOfDay(new Date(item.getT() * 1000)).getTime() / 1000));
            }
            if (symbol.getType() == SymbolType.Forex.ordinal())
                data = data.stream().filter(f -> FormulaTool.isTrades(new Date(f.getT() * 1000))).collect(Collectors.toList());
            if (!data.isEmpty()) {
                this.insert(data, name);

            }
        }
    }

    @Override
    public void initTask() {

        taskList.add(new AggregateTask(LevelType.minute, this, new ArrayList<>(Arrays.asList(1, 5)), connect));
        taskList.add(new AggregateTask(LevelType.minute, this, new ArrayList<>(Arrays.asList(15, 30)), connect));
        taskList.add(new AggregateTask(LevelType.hour, this, Collections.singletonList(1), connect));
        taskList.add(new AggregateTask(LevelType.day, this, Collections.singletonList(1), connect));

        taskList.forEach(CoreHandler::addTask);
    }

    @Override
    public void loadData(ParseDTO parseDTO) {
        taskList.forEach(item -> item.addData(parseDTO));

    }

    @Override
    public String tableName(String symbol, LevelType levelType, Integer grade) {
        return tablePev.replace(one, symbol.toLowerCase()).replace(two, levelType.name()).replace(three, grade.toString());
    }

    @Override
    public void addStrategy(String symbol, BigDecimal num, Integer decimal) {

        stringRedisTemplate.opsForHash().put(strategyPev, symbol, num.toPlainString().concat(",").concat(decimal.toString()));
    }

    @Override
    public List<StrategyDTO> strategyInfo() {
        List<StrategyDTO> strategyDTOS = new ArrayList<>();
        stringRedisTemplate.opsForHash().entries(strategyPev).forEach((k, v) -> {
            Symbol byId = symbolService.getById(k.toString());
            QuoteDTO quote = symbolService.getQuote(byId.getBase(), byId.getQuote());
            BigDecimal oldPrice = new BigDecimal(quote.getP());
            BigDecimal newPrice = BigDecimal.ZERO;
            String val = v.toString();
            BigDecimal num = new BigDecimal(val.split(",")[0]);
            int decimal = Integer.parseInt(val.split(",")[1]);
            if (num.compareTo(BigDecimal.ZERO) != 0) {
                newPrice = oldPrice.subtract(num.divide(BigDecimal.TEN.pow(decimal), RoundingMode.DOWN));
            }
            StrategyDTO build = StrategyDTO.builder().oldPrice(newPrice).newPrice(oldPrice).decimal(decimal).spread(num).code(k.toString()).build();

            strategyDTOS.add(build);
        });

        return strategyDTOS;
    }

    @Override
    public void delStrategy(String symbol) {
        stringRedisTemplate.opsForHash().delete(strategyPev, symbol);
    }

    /**
     * 判断表是否存在
     *
     * @param name 表名
     * @return false or true
     */
    private boolean existTable(String name) {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + db + "' AND TABLE_NAME = '" + name + "' limit 1";
        Integer integer = readJdbcTemplate.queryForObject(sql, Integer.class);
        return integer != null && integer == 1;
    }

    /**
     * 创建表
     *
     * @param name 表名
     */
    private void createTable(String name) {

        String sql = "CREATE TABLE `aggregate`.`" + name + "` (\n" + "  `t` int NOT NULL,\n" + "  `c` VARCHAR(25) NOT NULL,\n" + "  `h` VARCHAR(25) NOT NULL,\n" + "  `l` VARCHAR(25) NOT NULL,\n" + "  `o` VARCHAR(25) NOT NULL,\n" + "  PRIMARY KEY (`t`));\n";
        writeJdbcTemplate.execute(sql);
    }

    /**
     * 插入数据
     *
     * @param list 实体
     * @param name 表名
     */
    private void insert(List<AggregatesDTO> list, String name) {
        String sqlPev = "INSERT INTO `" + db + "`.`" + name + "` (`t`, `c`, `h`, `l`, `o`) VALUES ";
        String sut = list.stream().map(item -> "(" + item.getT() + ", '" + item.getC().toPlainString() + "', '" + item.getH().toPlainString() + "', '" + item.getL().toPlainString() + "', '" + item.getO().toPlainString() + "')").collect(Collectors.joining(","));
        String sql = sqlPev.concat(sut).concat(";");
        writeJdbcTemplate.execute(sql);
    }

    /**
     * 更新时间 根据
     *
     * @param aggregatesDTO 新数据
     * @param name          表名
     */
    private void update(AggregatesDTO aggregatesDTO, String name) {
        String sql = "UPDATE `" + db + "`.`" + name + "` SET `c` = '" + aggregatesDTO.getC().toPlainString() + "', `h` = '" + aggregatesDTO.getH().toPlainString() + "', `l` = '" + aggregatesDTO.getL().toPlainString() + "', `o` = '" + aggregatesDTO.getO().toPlainString() + "' WHERE (`t` = " + aggregatesDTO.getT() + ");";
        writeJdbcTemplate.execute(sql);
    }

    /**
     * 查询数据
     *
     * @param time 时间
     * @param name 表名
     * @return 数据
     */
    public AggregatesDTO selectById(long time, String name) {
        String sql = "SELECT * FROM `" + db + "`.`" + name + "` where t=" + time + " limit 1 ;";
        try {
            return readJdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(AggregatesDTO.class));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }


    public AggregatesDTO exchangeData(BigDecimal price, String tableName, long time) {
        AggregatesDTO dto = selectById(time, tableName);
        if (dto == null) {
            dto = new AggregatesDTO();
            dto.setC(price);
            dto.setO(price);
            dto.setH(price);
            dto.setL(price);
            dto.setT(time);
            insert(Collections.singletonList(dto), tableName);
        } else {
            dto.setC(price);
            dto.setH(dto.getH().compareTo(price) > 0 ? dto.getH() : price);
            dto.setL(dto.getL().compareTo(price) < 0 ? dto.getL() : price);
            update(dto, tableName);
        }

        return dto;
    }

    public DateTime parseDate(Date date, Integer interval) {
        if (interval > 1) {
            int minute = DateUtil.minute(date);
            if (minute % interval != 0) {
                minute = -(minute % interval);
            }
            DateTime dateTime = DateUtil.beginOfMinute(date);
            return DateUtil.offsetMinute(dateTime, minute);
        } else {
            return DateUtil.beginOfMinute(date);
        }

    }

    @Override
    public List<AggregatesDTO> aggregatesList(String code, LevelType levelType, Integer grade, long startTime, long endTime, Integer limit) {

        String tableName = this.tableName(code, levelType, grade);
        String sql = "SELECT * FROM " + db + "." + tableName + " where t>=" + endTime + " and t<=" + startTime + " order by t desc  limit " + limit;
        log.warn("aggregatesList sql:{}", sql);
        try {
            return readJdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AggregatesDTO.class));
        } catch (EmptyResultDataAccessException e) {
            return Collections.emptyList();
        }
    }


    @Override
    public void internalSubscription() {
        dispatcher.subscribe(SymbolService.convert_dto, new AggregateHandler(this));
        List<Symbol> list = symbolService.list(new QueryWrapper<Symbol>().eq("has_self", 1));
        for (Symbol symbol : list) {
            String concat = symbol.getBase().concat(symbol.getQuote());
            dispatcher.subscribe("market.US." + concat + ".1", new SelfCoinHandler(this, symbolService));

        }
    }

    @Override
    public void clearOldData() {
        List<Symbol> list = symbolService.lambdaQuery().select(Symbol::getCode).list();

        list.forEach(item -> {
            clearOldData(this.tableName(item.getCode(), LevelType.day, 1));
            clearOldData(this.tableName(item.getCode(), LevelType.hour, 1));
            clearOldData(this.tableName(item.getCode(), LevelType.minute, 5));
            clearOldData(this.tableName(item.getCode(), LevelType.minute, 15));
            clearOldData(this.tableName(item.getCode(), LevelType.minute, 30));
            clearOldData(this.tableName(item.getCode(), LevelType.minute, 1));
        });
    }

    private void clearOldData(String tbName) {
        if (isTableExists(tbName)) {
            String sqlOne = "SELECT COUNT(*) FROM " + tbName;
            Integer count = readJdbcTemplate.queryForObject(sqlOne, Integer.class);

            if (count != null && count > 1500) {
                String deleteSql = "DELETE FROM " + tbName + " WHERE t < " + DateUtil.offsetMinute(new Date(), -1500).getTime() / 1000;
                writeJdbcTemplate.execute(deleteSql);
                log.info("{} 数量大于1500，清理数据", tbName);
            }
        } else {
            log.warn("表 {} 不存在", tbName);
        }
    }

    private boolean isTableExists(String tableName) {
        try {
            String sql = "SELECT 1 FROM " + tableName + " LIMIT 1";
            readJdbcTemplate.queryForObject(sql, Integer.class);
            return true;
        } catch (EmptyResultDataAccessException e) {
            // 表不存在时会抛出 EmptyResultDataAccessException 异常
            return false;
        } catch (Exception e) {
            // 其他异常，可能是语法错误等
            return false;
        }
    }


}
