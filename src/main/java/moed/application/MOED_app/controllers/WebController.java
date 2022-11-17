package moed.application.MOED_app.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import moed.application.MOED_app.Entities.TrendInfo;
import moed.application.MOED_app.business.DataAnalyzer;
import moed.application.MOED_app.components.AppConfig;
import org.apache.commons.io.file.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.ArrayList;

import static org.springframework.http.HttpStatus.OK;

@Controller
public class WebController {
    @Autowired
    private AppConfig.SampleData sampleData;
    @GetMapping("main")
    public String display(Model model) throws InterruptedException, IOException {
        try {
            PathUtils.cleanDirectory(moed.application.MOED_app.business.IOC.getCleanPath());
        } catch(IOException IOE) { }
        ArrayList<TrendInfo> info = new ArrayList<>();
        for (var trend : sampleData.getTRENDS().values()) {
            info.add(new TrendInfo(trend));
        }
        model.addAttribute("infos", info);
        return "index";
    }

    @GetMapping(value = "test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> test() {
        String response = "";
        JsonArray jArr = new JsonArray();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (var trend : sampleData.getTRENDS().values()) {
            jArr.add(gson.toJsonTree(DataAnalyzer.Statistics.getStats(trend.getSeries())));
        }
        response = gson.toJson(jArr);
        return ResponseEntity.status(OK).body(response);
    }
}
