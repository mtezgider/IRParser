/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.murattezgider.irhtmlparser;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author murattezgider@gmail.com
 */
public class Parser {

    public static void main(String[] args) {
        Parser p = new Parser();
        // input directory
        List<File> fileList = new ArrayList<>();
        p.getFilesRecursive(new File("/home/hduser/Desktop/bil625_data/1  Murat Tezgider/"), fileList);

       // parse işlemi
        HashMap<String, DocumentFields> hashMap = new HashMap<>();
        for (File file : fileList) {
            p.parseDocument(file,hashMap);
        }

        // json verisine dönüştürme
        String result = p.toJSON(new ArrayList<>(hashMap.values()));
        System.out.println(result);

        // Json verisini yazma 
        File outputFile = new File("/home/hduser/Desktop/bil625_data/output.txt");
        if (!outputFile.exists()) {
            try {
                outputFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (PrintWriter out = new PrintWriter(outputFile)) {
            out.println(result);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * List<DocumentFields> listesini json verisine dönüştürür.
     *
     * @param list
     * @return
     */
    public String toJSON(List<DocumentFields> list) {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (DocumentFields d : list) {
            sb.append(",\n" + gson.toJson(d));
        }
        sb.append("]");
        
        return sb.toString().replaceFirst(",","");
    }

    /**
     * Verilen dizin ve dizinin alt dizinlerinde bulunan dosyaları listeler.
     *
     * @param pFile
     * @param fileList
     */
    private void getFilesRecursive(File pFile, List<File> fileList) {
        for (File files : pFile.listFiles()) {
            if (files.isDirectory()) {
                getFilesRecursive(files, fileList);
            } else {
                fileList.add(files);
            }
        }
    }

    /**
     * Verilen dosyayı parse eder, dosyadan parse edilen verileri veri yapısına aktardıktan sonra 
     *  bir HashMap'e ekler.
     * 
     *
     * @param file
     * @return
     */
    public HashMap<String, DocumentFields> parseDocument(File file,HashMap<String, DocumentFields> documentFieldHashMap) {

        try {
            Document doc = null;
            doc = Jsoup.parse(file, "utf-8");
            Elements recipes = doc.getElementsByClass("recipe-container-outer");

            for (Element recipe : recipes) {
                DocumentFields documentFields = new DocumentFields();
                if (doc.getElementById("canonicalUrl") != null) {
                    documentFields.setPath(doc.getElementById("canonicalUrl").attr("href"));
                } else {
                    documentFields.setPath(recipe.getElementsByClass("recipe-summary__h1").html());
                }

                documentFields.setTitle(recipe.getElementsByClass("recipe-summary__h1").html());
                
                 // for duplicate
                if (documentFieldHashMap.containsKey(documentFields.getTitle())) {
                    continue;
                }
                
                documentFields.setDescription(recipe.getElementsByClass("submitter__description").html());
                documentFields.setSubmitter(recipe.getElementsByClass("submitter__name").html());

                Elements ingredientElements = recipe.getElementsByAttributeValue("itemprop", "ingredients");
                for (Element element : ingredientElements) {
                    documentFields.getIngredients().add(element.html());
                }

                Element servingElement = recipe.getElementsByAttributeValue("itemprop", "recipeYield").first();
                if (servingElement != null) {
                    documentFields.setServings(servingElement.attr("content"));
                }

                Element caloriesElement = recipe.getElementsByAttributeValue("itemprop", "calories").first();
                if (caloriesElement != null) {
                    documentFields.getNutrition().setCalorie(caloriesElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Element fatElement = recipe.getElementsByAttributeValue("itemprop", "fatContent").first();
                if (fatElement != null) {
                    documentFields.getNutrition().setFat(fatElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Element carbohydrateElement = recipe.getElementsByAttributeValue("itemprop", "carbohydrateContent").first();
                if (carbohydrateElement != null) {
                    documentFields.getNutrition().setCarbs(carbohydrateElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Element proteinElement = recipe.getElementsByAttributeValue("itemprop", "proteinContent").first();
                if (proteinElement != null) {
                    documentFields.getNutrition().setProtein(proteinElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Element cholesterolElement = recipe.getElementsByAttributeValue("itemprop", "cholesterolContent").first();
                if (cholesterolElement != null) {
                    documentFields.getNutrition().setCholesterol(cholesterolElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Element sodiumElement = recipe.getElementsByAttributeValue("itemprop", "sodiumContent").first();
                if (sodiumElement != null) {
                    documentFields.getNutrition().setCholesterol(sodiumElement.html().replace("span", "").replace("<", "").replace(">", "").replace("/", ""));
                }

                Elements recipeInstructionsElements = recipe.getElementsByAttributeValue("itemprop", "recipeInstructions");
                for (Element element : recipeInstructionsElements) {
                    Elements subElement = element.getElementsByClass("recipe-directions__list--item");
                    for (Element element1 : subElement) {
                        documentFields.getDirections().add(element1.html());
                    }
                }

                Elements categoryElements = recipe.getElementsByClass("breadcrumbs");
                for (Element element : categoryElements) {
                    Elements subElements = element.children();
                    for (Element subElement : subElements) {
                        if (subElement.hasAttr("data-list-item")) {
                            documentFields.getCategoryList().add(subElement.getElementsByAttributeValue("itemprop", "title").html());
                        }
                    }
                }

                documentFieldHashMap.put(documentFields.getTitle(), documentFields);

            }

        } catch (IOException ex) {
            Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return documentFieldHashMap;
    }
}
