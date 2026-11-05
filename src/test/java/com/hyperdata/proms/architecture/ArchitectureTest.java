package com.hyperdata.proms.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = "com.hyperdata.proms",
    importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_repositories = noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule facades_must_be_interfaces = classes()
        .that()
        .resideInAPackage("com.hyperdata.proms.module.*.*.facade")
        .should()
        .beInterfaces();

    @ArchTest
    static final ArchRule facade_implementations_must_reside_in_internal_and_be_component = classes().that()
        .resideInAPackage("..facade.internal..")
        .should()
        .beAnnotatedWith(Component.class);

    @ArchTest
    static final ArchRule facades_must_not_return_entities = methods()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..facade..")
        .should()
        .notHaveRawReturnType(resideInAPackage("..entity.."));

    @ArchTest
    static final ArchRule controllers_must_be_annotated_with_rest_controller = classes().that()
        .resideInAPackage("..controller..")
        .and()
        .areNotInterfaces()
        .should()
        .beAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule controllers_must_implement_api_interface = classes().that()
        .resideInAPackage("com.hyperdata.proms.module..controller..")
        .and()
        .areNotInterfaces()
        .should()
        .implement(resideInAPackage("com.hyperdata.proms.module..controller.."));

    @ArchTest
    static final ArchRule services_must_be_annotated_with_service = classes().that()
        .resideInAPackage("..service..")
        .should()
        .beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule no_fields_should_be_annotated_with_autowired = noFields()
        .should()
        .beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule entities_must_not_be_annotated_with_lombok_data = noClasses()
        .that()
        .resideInAPackage("..entity..")
        .should()
        .beAnnotatedWith("lombok.Data");

    @ArchTest
    static final ArchRule controllers_must_not_be_annotated_with_transactional = noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule controller_methods_must_not_be_annotated_with_transactional = noMethods()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule dtos_must_be_records = classes()
        .that()
        .resideInAPackage("..dto..")
        .and()
        .areNotAnonymousClasses()
        .should()
        .beRecords();

    @ArchTest
    static final ArchRule modules_must_not_access_internal_packages_of_other_modules = classes()
        .that()
        .resideInAPackage("com.hyperdata.proms..")
        .should(new ArchCondition<JavaClass>("only access public facades and facade DTOs of other modules") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                String callerPackage = javaClass.getPackageName();
                String callerFeature = extractFeaturePackage(callerPackage);

                for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                    JavaClass targetClass = dependency.getTargetClass();
                    String targetPackage = targetClass.getPackageName();
                    String targetFeature = extractFeaturePackage(targetPackage);

                    if (targetFeature != null && !targetFeature.equals(callerFeature)) {
                        boolean isFacade = targetPackage.equals(targetFeature + ".facade");
                        boolean isFacadeDto = targetPackage.startsWith(targetFeature + ".facade.dto");

                        if (!isFacade && !isFacadeDto) {
                            String message = String.format(
                                "Class %s illegally accesses %s in internal package %s (cross-module access must go through %s.facade or %s.facade.dto)",
                                javaClass.getName(),
                                targetClass.getName(),
                                targetPackage,
                                targetFeature,
                                targetFeature);
                            events.add(SimpleConditionEvent.violated(dependency, message));
                        }
                    }
                }
            }
        });

    private static String extractFeaturePackage(String packageName) {
        if (!packageName.startsWith("com.hyperdata.proms.module.")) {
            return null;
        }
        String rest = packageName.substring("com.hyperdata.proms.module.".length());
        String[] parts = rest.split("\\.");
        if (parts.length >= 2) {
            return "com.hyperdata.proms.module." + parts[0] + "." + parts[1];
        }
        return null;
    }
}
