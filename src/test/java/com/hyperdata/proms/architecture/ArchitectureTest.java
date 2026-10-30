package com.hyperdata.proms.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "com.hyperdata.proms",
        importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_repositories =
            noClasses()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule facades_must_be_interfaces =
            classes()
                    .that()
                    .resideInAPackage("com.hyperdata.proms.module.*.*.facade")
                    .should()
                    .beInterfaces();

    @ArchTest
    static final ArchRule facade_implementations_must_reside_in_internal_and_be_component =
            classes()
                    .that()
                    .resideInAPackage("..facade.internal..")
                    .should()
                    .beAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule facades_must_not_return_entities =
            methods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage("..facade..")
                    .should()
                    .notHaveRawReturnType(resideInAPackage("..entity.."));

    @ArchTest
    static final ArchRule controllers_must_be_annotated_with_rest_controller =
            classes()
                    .that()
                    .resideInAPackage("..controller..")
                    .should()
                    .beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule services_must_be_annotated_with_service =
            classes()
                    .that()
                    .resideInAPackage("..service..")
                    .should()
                    .beAnnotatedWith(Service.class);
}
