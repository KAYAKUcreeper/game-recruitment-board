package com.gamerecruitment.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class DependencyTest {
  @Test
  void innerLayersDoNotDependOnTechnology() {
    var classes = new ClassFileImporter().importPackages("com.gamerecruitment");
    noClasses()
        .that()
        .resideInAnyPackage("..domain..", "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..", "java.sql..", "..adapter..", "..configuration..")
        .allowEmptyShould(true)
        .check(classes);
  }
}
