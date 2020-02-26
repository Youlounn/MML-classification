library(readr)
library(caret)
library(randomForest)
library(LogicReg)
library(e1071)
library(party)
data <- read_csv("output_LAFONT_LEMANCEL_MANDE_RIALET/iris.csv")
predictive <- names(data[dim(data)[2]])
predictors <- "."
formula <- reformulate(termlabels = predictors, response = predictive)
fitControl <- trainControl(method="cv", number=10)
data_train <- data
data_test <- data
model <- train(formula, data=data_train,method="rf",trControl=fitControl)
pred <- predict(model,newdata=data_test)
mat <- confusionMatrix(table(pred,data_test[[predictive]]))
print("Balanced Accuracy")
print(mat$byClass[,"Balanced Accuracy"])
print("Recall")
print(mat$byClass[,"Recall"])
print("Precision")
print(mat$byClass[,"Precision"])
print("F1")
print(mat$byClass[,"F1"])
print("Accuracy")
print(mat$overall["Accuracy"])
print("Macro Recall")
print(mean(mat$byClass[,"Recall"],na.rm=TRUE))
print("Macro Precision")
print(mean(mat$byClass[,"Precision"],na.rm=TRUE))
print("Macro F1")
print(mean(mat$byClass[,"F1"],na.rm=TRUE))
print("Macro Accuracy non supporté")
