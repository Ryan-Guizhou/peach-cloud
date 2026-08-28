package com.peach.common;


import com.peach.common.util.StringUtil;

import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;


/**
 * Dao层自动生成工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 * @Description Dao层自动生成工具类
 */
@Slf4j
public class MapperGenerator {
	private static final String lineSeparator = System.lineSeparator();

	private static final String INSERT_SELECTIVE_COLUMN_SQL = "insertSelectiveColumn";

	private static final String INSERT_SELECTIVE_VALUE_SQL = "insertSelectiveValue";

	private static final String UPDATE_SELECTIVE_COLUMN_SQL = "updateSelectiveColumn";

	private static final String UPDATE_SELECTIVE_VALUE_SQL = "updateSelectiveValue";

	private static final String DEFAULT_MAPPER_NAME = "GeneratorDao";

	private static final Path DEFAULT_OUTPUT_PATH = Path.of(System.getProperty("user.dir"), "src/main/java/generator");

	public static <T> String genMapper(Class<T> c) {
		return genMapper(c, false);
	}

	public static <T> void genMapperToFile(Class<T> c, String mapperName, String ouptPutPach) {

		String content = genMapper(c);
		Path outputDir = StringUtil.isBlank(ouptPutPach) ? DEFAULT_OUTPUT_PATH : Path.of(ouptPutPach);

		if (StringUtil.isBlank(mapperName)){
			mapperName = DEFAULT_MAPPER_NAME;
		}
		Path file = outputDir.resolve(mapperName + ".xml");
		if (Files.exists(file)) {
            log.info("Mapper file already exists: {}", file.toAbsolutePath());
			return;
		}

		try {
			if (!Files.exists(outputDir)) {
				Files.createDirectories(outputDir);
			}
			Files.writeString(file, content);
            log.info("Mapper file generated: {}", file.toAbsolutePath());
		} catch (IOException e) {
			log.error("Failed to write mapper file: {}", file.toAbsolutePath(), e);
		}
	}

	public static <T> String genMapper(Class<T> c, boolean isGenPrimaryKey) {
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(lineSeparator);
		builder.append(
						"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">")
				.append(lineSeparator);
		builder.append(String.format("<mapper namespace=\"%s\">", getMapperNamespace(c))).append(lineSeparator);
		builder.append(genAllColumn(c)).append(lineSeparator).append(lineSeparator);
		builder.append(genAllColumnAlias(c)).append(lineSeparator).append(lineSeparator);
		builder.append(genAllColumnValue(c)).append(lineSeparator).append(lineSeparator).append(lineSeparator);
		builder.append(genItemAllColumnValue(c)).append(lineSeparator).append(lineSeparator).append(lineSeparator);
		builder.append(genAllColumnSet(c)).append(lineSeparator).append(lineSeparator).append(lineSeparator);
//		builder.append(genInsertSelectiveColumn(c)).append(lineSeparator).append(lineSeparator);
//		builder.append(genInsertSelectiveValue(c)).append(lineSeparator).append(lineSeparator);
		builder.append(genUpdateSelectiveColumn(c)).append(lineSeparator).append(lineSeparator);
		builder.append(genUpdateSelectiveValue(c)).append(lineSeparator).append(lineSeparator);
		if (isGenPrimaryKey) {
			builder.append(genPrimaryKeyColumnCond(c)).append(lineSeparator).append(lineSeparator);
		}
		builder.append(genAllColumnCond(c)).append(lineSeparator).append(lineSeparator);
		// builder.append(genResultMap(c)).append(lineSeparator);
		builder.append(insert(c)).append(lineSeparator).append(lineSeparator);
		builder.append(batchInsert(c)).append(lineSeparator).append(lineSeparator);
		builder.append(update(c)).append(lineSeparator).append(lineSeparator);
//		builder.append(insertSelective(c)).append(lineSeparator).append(lineSeparator);
		builder.append(delById(c)).append(lineSeparator).append(lineSeparator);
		builder.append(delByIds(c)).append(lineSeparator).append(lineSeparator);
		builder.append(del(c)).append(lineSeparator).append(lineSeparator);
		if (isGenPrimaryKey) {
			builder.append(delByPrimaryKey(c)).append(lineSeparator).append(lineSeparator);
		}
		builder.append(updateById(c)).append(lineSeparator).append(lineSeparator);
		if (isGenPrimaryKey) {
			builder.append(updateByPrimaryKey(c)).append(lineSeparator).append(lineSeparator);
		}
		builder.append(selectById(c)).append(lineSeparator).append(lineSeparator);
		builder.append(selectByIds(c)).append(lineSeparator).append(lineSeparator);
		builder.append(select(c)).append(lineSeparator).append(lineSeparator);
		builder.append(count(c)).append(lineSeparator);
		builder.append("</mapper>");
		return builder.toString();
	}

	public static <T> String genAllColumn(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"allColumn\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (i % 4 == 1) {
				fb.append("    ");
			}
			if (i == fields.size()) {
				fb.append(String.format("%-50s", f.getAnnotation(Column.class).name()));
			} else {
				fb.append(String.format("%-50s", f.getAnnotation(Column.class).name() + ","));
			}

			if (i % 4 == 0) {
				fb.append(lineSeparator);
			}
		}
		builder.append(fb.toString()).append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genAllColumnAlias(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"allColumnAlias\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (i % 4 == 1) {
				fb.append("    ");
			}
			if (i == fields.size()) {
				fb.append(String.format("%-50s", f.getAnnotation(Column.class).name() + " as " + f.getName()));
			} else {
				fb.append(String.format("%-50s", f.getAnnotation(Column.class).name() + " as " + f.getName() + ","));
			}

			if (i % 4 == 0) {
				fb.append(lineSeparator);
			}
		}
		builder.append(fb.toString()).append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genAllColumnValue(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"allColumnValue\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (i % 4 == 1) {
				fb.append("    ");
			}
			String param = String.format("#{%s,jdbcType=%s}", f.getName(), getJdbcType(f));
			if (i == fields.size()) {
				fb.append(String.format("%-40s", param));
			} else {
				fb.append(String.format("%-40s", param + ","));
			}

			if (i % 4 == 0) {
				fb.append(lineSeparator);
			}
		}
		builder.append(fb.toString()).append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genItemAllColumnValue(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"itemAllColumnValue\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (i % 4 == 1) {
				fb.append("    ");
			}
			String param = String.format("#{item.%s,jdbcType=%s}", f.getName(), getJdbcType(f));
			if (i == fields.size()) {
				fb.append(String.format("%-40s", param));
			} else {
				fb.append(String.format("%-40s", param + ","));
			}

			if (i % 4 == 0) {
				fb.append(lineSeparator);
			}
		}
		builder.append(fb.toString()).append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genAllColumnSet(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"allColumnSet\">").append(lineSeparator);
		builder.append("    ");
		builder.append("<trim suffixOverrides=\",\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		Field idField = fields.stream().filter(f -> f.getAnnotation(Id.class) != null).findFirst().get();
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (f == idField) {
				continue;
			}
			fb.append("        ");
			if (f.getType() != String.class) {
				fb.append(String.format("<if test=\"%s != null\">", f.getName())).append(lineSeparator);
			} else {
				fb.append(String.format("<if test=\"%s != null and %s != ''\">", f.getName(), f.getName()))
						.append(lineSeparator);
			}
			String param = String.format("            %s = #{%s,jdbcType=%s}", f.getAnnotation(Column.class).name(),
					f.getName(), getJdbcType(f));
			fb.append(String.format("%-50s", param + ",")).append(lineSeparator);
			fb.append("        </if>");
			fb.append(lineSeparator);
		}
		builder.append(fb.toString());
		builder.append("    ");
		builder.append("</trim>");
		builder.append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}


	public static <T> String genInsertSelectiveColumn(Class<T> c) {
		return genColumnSelective(c, Boolean.FALSE, INSERT_SELECTIVE_COLUMN_SQL);
	}

	public static <T> String genInsertSelectiveValue(Class<T> c) {
		return genColumnSelectiveValue(c, Boolean.FALSE, INSERT_SELECTIVE_VALUE_SQL);
	}

	public static <T> String genUpdateSelectiveColumn(Class<T> c) {
		return genColumnSelective(c, Boolean.TRUE, UPDATE_SELECTIVE_COLUMN_SQL);
	}

	public static <T> String genUpdateSelectiveValue(Class<T> c) {
		return genColumnSelectiveValue(c, Boolean.TRUE, UPDATE_SELECTIVE_VALUE_SQL);
	}

	private static <T> String genColumnSelective(Class<T> c, Boolean isUpdate, String sqlId) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"").append(sqlId).append("\">").append(lineSeparator);
		builder.append("    ");
		builder.append("<trim suffixOverrides=\",\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		Field idField = fields.stream().filter(f -> f.getAnnotation(Id.class) != null).findFirst().get();
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (isUpdate && f == idField) {
				continue;
			}
			fb.append("        ");
			if (f.getType() != String.class) {
				fb.append(String.format("<if test=\"%s != null\">", f.getName())).append(lineSeparator);
			} else {
				fb.append(String.format("<if test=\"%s != null and %s != ''\">", f.getName(), f.getName()))
						.append(lineSeparator);
			}
			String param = String.format("            %s", f.getAnnotation(Column.class).name());
			fb.append(String.format("%-50s", param + ",")).append(lineSeparator);
			fb.append("        </if>");
			fb.append(lineSeparator);
		}
		builder.append(fb.toString());
		builder.append("    ");
		builder.append("</trim>");
		builder.append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	private static <T> String genColumnSelectiveValue(Class<T> c, Boolean isUpdate, String sqlId) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"" + sqlId + "\">").append(lineSeparator);
		builder.append("    ");
		builder.append("<trim suffixOverrides=\",\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		Field idField = fields.stream().filter(f -> f.getAnnotation(Id.class) != null).findFirst().get();
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			if (isUpdate && f == idField) {
				continue;
			}
			fb.append("        ");
			if (f.getType() != String.class) {
				fb.append(String.format("<if test=\"%s != null\">", f.getName())).append(lineSeparator);
			} else {
				fb.append(String.format("<if test=\"%s != null and %s != ''\">", f.getName(), f.getName()))
						.append(lineSeparator);
			}
			String param = String.format("            #{%s,jdbcType=%s}", f.getName(), getJdbcType(f));
			fb.append(String.format("%-50s", param + ",")).append(lineSeparator);
			fb.append(String.format("        </if>")).append(lineSeparator);
		}
		builder.append(fb.toString());
		builder.append("    ");
		builder.append("</trim>");
		builder.append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genPrimaryKeyColumnCond(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"primaryKeyColumnCond\">").append(lineSeparator);
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String genAllColumnCond(Class<T> c) {
		StringBuilder builder = new StringBuilder();
		builder.append("<sql id=\"allColumnCond\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		StringBuilder fb = new StringBuilder();
		Field f = null;
		for (int i = 1; i <= fields.size(); i++) {
			f = fields.get(i - 1);
			fb.append("    ");
			if (f.getType() != String.class) {
				fb.append(String.format("<if test=\"%s != null\">", f.getName())).append(lineSeparator);
			} else {
				fb.append(String.format("<if test=\"%s != null and %s != ''\">", f.getName(), f.getName()))
						.append(lineSeparator);
			}
			String param = String.format("        AND %s = #{%s,jdbcType=%s}", f.getAnnotation(Column.class).name(),
					f.getName(), getJdbcType(f));
			fb.append(String.format("%-50s", param)).append(lineSeparator);
			fb.append(String.format("    </if>"));
			fb.append(lineSeparator);
		}
		builder.append(fb.toString());
		builder.append("</sql>");
		return builder.toString();
	}

	public static <T> String insert(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append("<insert id=\"insert\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append(String.format("    INSERT INTO %s (", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumn")).append(lineSeparator);
		builder.append("    ) VALUES (").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnValue")).append(lineSeparator);
		builder.append("    )").append(lineSeparator);
		builder.append("</insert>");

		return builder.toString();
	}

	public static <T> String batchInsert(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append("<insert id=\"batchInsert\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append(String.format("    INSERT INTO %s (", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumn")).append(lineSeparator);
		builder.append("    ) VALUES ").append(lineSeparator);
		builder.append("    <foreach collection=\"list\" index=\"index\" item=\"item\" separator=\",\">").append(lineSeparator);
		builder.append(String.format("        (<include refid=\"%s\"/>)", "itemAllColumnValue")).append(lineSeparator);
		builder.append("    </foreach>").append(lineSeparator);
		builder.append("</insert>");

		return builder.toString();
	}

	public static <T> String update(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append("<update id=\"update\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append(String.format("    UPDATE %s SET", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnSet")).append(lineSeparator);
		builder.append("    <where>").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnCond")).append(lineSeparator);
		builder.append("    </where>").append(lineSeparator);
		builder.append("</update>");

		return builder.toString();
	}

	public static <T> String insertSelective(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append("<insert id=\"insertSelective\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append(String.format("    INSERT INTO %s (", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", INSERT_SELECTIVE_COLUMN_SQL)).append(lineSeparator);
		builder.append("    ) VALUES (").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", INSERT_SELECTIVE_VALUE_SQL)).append(lineSeparator);
		builder.append("    )").append(lineSeparator);
		builder.append("</insert>");

		return builder.toString();
	}

	public static <T> String updateById(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<update id=\"updateById\" parameterType=\"java.lang.String\">").append(lineSeparator);
		builder.append(String.format("    UPDATE %s SET ", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnSet")).append(lineSeparator);
		builder.append(String.format("    WHERE %s = %s ", idField.getAnnotation(Column.class).name(),
				" #{" + idField.getName() +",jdbcType="+getJdbcType(idField)+ "}")).append(lineSeparator);
		builder.append("</update>");

		return builder.toString();
	}

	public static <T> String updateByPrimaryKey(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<update id=\"updateByPrimaryKey\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append(String.format("    UPDATE %s SET ", tableName)).append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnSet")).append(lineSeparator);
		builder.append("    WHERE 1=1 ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "primaryKeyColumnCond")).append(lineSeparator);
		builder.append("</update>");
		return builder.toString();
	}

	public static <T> String selectById(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<select id=\"selectById\" parameterType=\"string\" resultType=\"" + c.getName() + "\">")
				.append(lineSeparator);
		builder.append("    SELECT ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnAlias")).append(lineSeparator);
		builder.append("    FROM " + tableName + " ").append(lineSeparator);
		builder.append(String.format("    WHERE %s = %s", idField.getAnnotation(Column.class).name(), "#{value,jdbcType="+getJdbcType(idField)+"}"))
				.append(lineSeparator);
		builder.append("</select>");

		return builder.toString();
	}

	public static <T> String selectByIds(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append(
						"<select id=\"selectByIds\" parameterType=\"java.util.List\" resultType=\"" + c.getName() + "\">")
				.append(lineSeparator);
		builder.append("    SELECT ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnAlias")).append(lineSeparator);
		builder.append("    FROM " + tableName + " ").append(lineSeparator);
		builder.append(String.format("    WHERE %s IN ", idField.getAnnotation(Column.class).name()))
				.append(lineSeparator);
		builder.append(
						"    <foreach collection=\"list\" index=\"index\" item=\"id\" open=\"(\" separator=\",\" close=\")\">")
				.append(lineSeparator);
		builder.append(String.format("        #{id,jdbcType=%s}", getJdbcType(idField))).append(lineSeparator);
		builder.append("    </foreach>").append(lineSeparator);
		builder.append("</select>");
		return builder.toString();
	}

	public static <T> String select(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append(
						"<select id=\"select\" parameterType=\"" + c.getName() + "\" resultType=\"" + c.getName() + "\">")
				.append(lineSeparator);
		builder.append("    SELECT ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnAlias")).append(lineSeparator);
		builder.append("    FROM " + tableName + " ").append(lineSeparator);
		builder.append("    <where>").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnCond")).append(lineSeparator);
		builder.append("    </where>").append(lineSeparator);
		builder.append("</select>");
		return builder.toString();
	}

	public static <T> String selectByCondition(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append(
						"<select id=\"selectByCondition\" parameterType=\"" + c.getName() + "\" resultType=\"" + c.getName() + "\">")
				.append(lineSeparator);
		builder.append("    SELECT ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnAlias")).append(lineSeparator);
		builder.append("    FROM " + tableName + " ").append(lineSeparator);
		builder.append("    WHERE 1=1 ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnCond")).append(lineSeparator);
		builder.append("</select>");
		return builder.toString();
	}

	public static <T> String count(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		StringBuilder builder = new StringBuilder();
		builder.append("<select id=\"count\" parameterType=\"" + c.getName() + "\" resultType=\"java.lang.Integer\">")
				.append(lineSeparator);
		builder.append("    SELECT COUNT(1) ").append(lineSeparator);
		builder.append("    FROM " + tableName + " ").append(lineSeparator);
		builder.append("    <where>").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnCond")).append(lineSeparator);
		builder.append("    </where>").append(lineSeparator);
		builder.append("</select>");
		return builder.toString();
	}

	public static <T> String delById(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<delete id=\"delById\" parameterType=\"string\">").append(lineSeparator);
		builder.append("    DELETE FROM " + tableName).append(lineSeparator);
		builder.append("    WHERE ");
		builder.append(String.format("%s = #{value,jdbcType=%s}", idField.getAnnotation(Column.class).name(),getJdbcType(idField)))
				.append(lineSeparator);
		builder.append("</delete>");
		return builder.toString();
	}

	public static <T> String delByIds(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<delete id=\"delByIds\" parameterType=\"java.util.List\">").append(lineSeparator);
		builder.append("    DELETE FROM " + tableName).append(lineSeparator);
		builder.append(String.format("    WHERE %s IN ", idField.getAnnotation(Column.class).name()))
				.append(lineSeparator);
		builder.append(
						"    <foreach collection=\"list\" index=\"index\" item=\"id\" open=\"(\" separator=\",\" close=\")\">")
				.append(lineSeparator);
		builder.append(String.format("        #{id,jdbcType=%s}", getJdbcType(idField))).append(lineSeparator);
		builder.append("    </foreach>").append(lineSeparator);
		builder.append("</delete>");
		return builder.toString();
	}

	public static <T> String del(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<delete id=\"del\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append("    DELETE FROM " + tableName).append(lineSeparator);
		builder.append("    <where>").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "allColumnCond")).append(lineSeparator);
		builder.append("    </where>").append(lineSeparator);
		builder.append("</delete>");
		return builder.toString();
	}

	public static <T> String delByPrimaryKey(Class<T> c) {
		String tableName = c.getAnnotation(Table.class).name();
		Field idField = getAllColumnField(c).stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst().get();
		StringBuilder builder = new StringBuilder();
		builder.append("<delete id=\"delByPrimaryKey\" parameterType=\"" + c.getName() + "\">").append(lineSeparator);
		builder.append("    DELETE FROM " + tableName).append(lineSeparator);
		builder.append("    WHERE 1=1 ").append(lineSeparator);
		builder.append(String.format("        <include refid=\"%s\" />", "primaryKeyColumnCond")).append(lineSeparator);
		builder.append("</delete>");
		return builder.toString();
	}

	public static <T> String selectWithPage(Class<T> c) {
		return "";
	}

	public static <T> String genResultMap(Class<T> c) {
		StringBuilder b = new StringBuilder();
		b.append("<resultMap id=\"" + c.getSimpleName() + "Map\" type=\"" + c.getName() + "\">").append(lineSeparator);
		List<Field> fields = getAllColumnField(c);
		Optional<Field> idField = fields.stream().filter(f -> {
			return f.getAnnotation(Id.class) != null;
		}).findFirst();
		idField.ifPresent(field -> {
			b.append(String.format("    <id %-30s %-30s />",
					"column=\"" + field.getAnnotation(Column.class).name() + "\"",
					"property=\"" + field.getName() + "\""));
			b.append(lineSeparator);
		});
		fields.stream().map(f -> {
			String jdbcType = getJdbcType(f);
			return String.format("    <result %-30s %-30s %-20s />",
					"column=\"" + f.getAnnotation(Column.class).name() + "\"", "property=\"" + f.getName() + "\"",
					"jdbcType=\"" + jdbcType + "\"");
		}).forEach(s -> {
			b.append(s);
			b.append(lineSeparator);
		});
		b.append("</resultMap>").append(lineSeparator);
		return b.toString();
	}

	public static List<Field> getAllColumnField(Class clazz) {
		List<Field> fieldList = new ArrayList<>();
		while (clazz != null && clazz != Object.class) {
			Field[] fields = clazz.getDeclaredFields();
			fieldList.addAll(Arrays.asList(fields));
			clazz = clazz.getSuperclass();
		}
		//鏉╁洦鎶ゆ惔蹇撳灙閸栨牕鐡у▓?
		fieldList.remove(0);
		return fieldList ;
	}

	private static <T> String getMapperNamespace(Class<T> c) {
		String simpleName = c.getSimpleName();
		if ("UserOperLogDO".equals(simpleName)) {
			return "com.peach.userservice.dao.UserOperLogDao";
		}
		String daoSimpleName = simpleName.endsWith("DO") ? simpleName.substring(0, simpleName.length() - 2) + "Dao"
				: simpleName + "Dao";
		String basePackage = c.getPackage().getName().replace(".entity", ".dao");
		return basePackage + "." + daoSimpleName;
	}

	private static String getJdbcType(Field f) {
		Class type = f.getType();
		if (type.equals(String.class)) {
			return "VARCHAR";
		} else if (type.equals(BigDecimal.class)) {
			return "DECIMAL";
		} else if (type.equals(Integer.class)) {
			return "INTEGER";
		} else if (type.equals(Date.class)) {
			return "TIMESTAMP";
		} else if (type.equals(LocalDateTime.class) || type.equals(Instant.class)) {
			return "TIMESTAMP";
		} else if (type.equals(LocalDate.class)) {
			return "DATE";
		} else if (type.equals(Long.class)) {
			return "BIGINT";
		} else if (type.equals(Double.class) || type.equals(Float.class)) {
			return "DOUBLE";
		} else {
			return "VARCHAR";
		}
	}
}
