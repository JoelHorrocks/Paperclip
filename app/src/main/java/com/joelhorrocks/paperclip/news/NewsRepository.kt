package com.joelhorrocks.paperclip.news

import com.joelhorrocks.paperclip.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: add datasources - for now they are fake
class NewsRepository() {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val mockArticles = listOf(
        Article(
            id = 1,
            headline = "Breakthrough in Quantum Computing Achieved by Tech Giants",
            description = "Researchers at major technology companies have successfully demonstrated a new quantum computing architecture that could revolutionize data processing and encryption.",
            publicationDate = dateFmt.parse("2024-07-10"),
            readTimeMin = 8,
            publisher = "Tech Pulse",
            imageResource = R.mipmap.placeholder_news_quantum,
            url = "https://example.com/quantum-computing-breakthrough"
        ),
        Article(
            id = 2,
            headline = "Climate Change Impacts on Global Food Security",
            description = "New study reveals how rising temperatures and changing weather patterns are affecting crop yields worldwide, with potential solutions for sustainable agriculture.",
            publicationDate = dateFmt.parse("2024-07-08"),
            readTimeMin = 12,
            publisher = "Green Planet Daily",
            imageResource = R.mipmap.placeholder_news_crops,
            url = "https://example.com/climate-food-security"
        ),
        Article(
            id = 3,
            headline = "The Rise of Remote Work: 5 Years Later",
            description = "An analysis of how remote work has evolved since the pandemic, examining productivity trends, employee satisfaction, and the future of hybrid work models.",
            publicationDate = dateFmt.parse("2024-07-15"),
            readTimeMin = 6,
            publisher = "Workflow Today",
            imageResource = R.mipmap.placeholder_news_desk,
            url = "https://example.com/remote-work-evolution"
        ),
        Article(
            id = 4,
            headline = "Artificial Intelligence in Healthcare: Diagnosis Revolution",
            description = "Medical professionals are increasingly using AI-powered tools to improve diagnostic accuracy and speed, leading to better patient outcomes across various specialties.",
            publicationDate = dateFmt.parse("2024-07-12"),
            readTimeMin = 10,
            publisher = "MedTech Insight",
            imageResource = R.mipmap.placeholder_news_ai,
            url = "https://example.com/ai-healthcare-diagnosis"
        ),
        Article(
            id = 5,
            headline = "Space Tourism Takes Off: First Commercial Lunar Mission",
            description = "Private space companies are preparing for the first commercial lunar tourism flights, marking a new era in space exploration and accessibility.",
            publicationDate = dateFmt.parse("2024-07-05"),
            readTimeMin = 7,
            publisher = "Cosmos Report",
            imageResource = R.mipmap.placeholder_news_rocket,
            url = "https://example.com/space-tourism-lunar"
        ),
        Article(
            id = 6,
            headline = "Electric Vehicle Adoption Reaches Tipping Point",
            description = "Global electric vehicle sales have surpassed traditional combustion engines in major markets, driven by improved battery technology and charging infrastructure.",
            publicationDate = dateFmt.parse("2024-07-14"),
            readTimeMin = 9,
            publisher = "New Auto",
            imageResource = R.mipmap.placeholder_news_electric_car,
            url = "https://example.com/electric-vehicle-adoption"
        ),
        Article(
            id = 7,
            headline = "Sustainable Fashion: The End of Fast Fashion Era",
            description = "Consumer behavior shifts toward sustainable and ethical fashion choices are forcing major retailers to reimagine their production and supply chain strategies.",
            publicationDate = dateFmt.parse("2024-07-06"),
            readTimeMin = 5,
            publisher = "StyleWatch",
            imageResource = R.mipmap.placeholder_news_clothes,
            url = "https://example.com/sustainable-fashion-trends"
        ),
        Article(
            id = 8,
            headline = "Mental Health Apps Show Promising Results in Clinical Trials",
            description = "Recent studies demonstrate that digital mental health interventions can be as effective as traditional therapy for treating anxiety and depression.",
            publicationDate = dateFmt.parse("2024-07-11"),
            readTimeMin = 11,
            publisher = "Mind Wellness",
            imageResource = R.mipmap.placeholder_news_phone,
            url = "https://example.com/mental-health-apps-study"
        ),
        Article(
            id = 9,
            headline = "Renewable Energy Storage Solutions Advance Rapidly",
            description = "Innovative battery technologies and alternative energy storage methods are solving the intermittency challenges of solar and wind power generation.",
            publicationDate = dateFmt.parse("2024-07-09"),
            readTimeMin = 8,
            publisher = "Energy NextGen",
            imageResource = R.mipmap.placeholder_news_renewable_energy,
            url = "https://example.com/renewable-energy-storage"
        ),
        Article(
            id = 10,
            headline = "Urban Farming Transforms City Landscapes",
            description = "Vertical farms and rooftop gardens are becoming integral to urban planning, providing fresh produce while reducing transportation costs and environmental impact.",
            publicationDate = dateFmt.parse("2024-07-13"),
            readTimeMin = 6,
            publisher = "City Life Chronicle",
            imageResource = R.mipmap.placeholder_news_advanced_farming,
            url = "https://example.com/urban-farming-revolution"
        )
    )

    suspend fun fetchLatestNews(): List<Article> {
        // TODO: remove loading simulation delay
        delay(2000)
        return mockArticles.shuffled().take(4)
    }
}